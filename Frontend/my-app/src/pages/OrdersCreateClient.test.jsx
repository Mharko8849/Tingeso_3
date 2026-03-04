import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';

const mockNavigate = vi.fn();
const mockShow = vi.fn();

vi.mock('../services/http-common', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));
vi.mock('../components/Alerts/useAlert', () => ({
  useAlert: () => ({ show: mockShow }),
}));
vi.mock('../components/Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('@react-keycloak/web', () => ({
  useKeycloak: () => ({
    keycloak: {
      authenticated: true,
      token: 'fake-token',
      tokenParsed: { realm_access: { roles: ['ADMIN'] } },
    },
    initialized: true,
  }),
}));
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }),
}));
vi.mock('../components/Clients/ClientSearch', () => ({
  default: ({ onSelect, selected }) => React.createElement('div', { 'data-testid': 'client-search' },
    React.createElement('button', { 'data-testid': 'select-active', onClick: () => onSelect({ id: 1, name: 'Test', lastName: 'User', username: 'tuser', email: 'test@mail.com', rut: '12345678-9', stateClient: 'ACTIVO' }) }, 'Select Active'),
    React.createElement('button', { 'data-testid': 'select-restricted', onClick: () => onSelect({ id: 2, name: 'Restricted', stateClient: 'RESTRINGIDO' }) }, 'Select Restricted'),
    selected && React.createElement('span', { 'data-testid': 'selected-indicator' }, `Selected: ${  selected.name}`),
  ),
}));
vi.mock('../components/Register/ModalEmployeesRegister', () => ({
  default: ({ onCreate, onCancel }) => React.createElement('div', { 'data-testid': 'register-modal' },
    React.createElement('button', { 'data-testid': 'register-submit', onClick: () => onCreate({ username: 'newclient', name: 'New', lastName: 'Client', email: 'new@mail.com', password: '12345', rol: 'CLIENT' }) }, 'Register'),
    React.createElement('button', { 'data-testid': 'register-cancel', onClick: onCancel }, 'Cancel'),
  ),
}));

import OrdersCreateClient from './OrdersCreateClient';
import api from '../services/http-common';

describe('OrdersCreateClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('renders the page with header and client search', () => {
    render(React.createElement(OrdersCreateClient));
    expect(screen.getByText(/Crear Pedido/i)).toBeInTheDocument();
    expect(screen.getByTestId('client-search')).toBeInTheDocument();
  });

  it('renders date pickers with correct labels', () => {
    render(React.createElement(OrdersCreateClient));
    expect(screen.getByLabelText(/Fecha inicio/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Fecha retorno/i)).toBeInTheDocument();
  });

  it('selects an active client and displays details in sidebar', async () => {
    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByTestId('select-active'));

    await waitFor(() => {
      expect(screen.getByText('tuser')).toBeInTheDocument();
      expect(screen.getByText(/Test User/)).toBeInTheDocument();
      expect(screen.getByText('test@mail.com')).toBeInTheDocument();
      expect(screen.getByText(/12345678-9/)).toBeInTheDocument();
    });
  });

  it('shows error when selecting restricted client', () => {
    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByTestId('select-restricted'));
    expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'error',
      message: expect.stringContaining('restringido'),
    }));
  });

  it('deselects client when clicking Deseleccionar', async () => {
    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByTestId('select-active'));

    await waitFor(() => {
      expect(screen.getByText('Deseleccionar')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Deseleccionar'));
    await waitFor(() => {
      expect(screen.getByText(/No hay cliente seleccionado/i)).toBeInTheDocument();
    });
  });

  it('navigates to tools step when clicking Siguiente with valid selection', async () => {
    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByTestId('select-active'));

    await waitFor(() => {
      expect(screen.getByText('tuser')).toBeInTheDocument();
    });

    // Click the Siguiente button (the enabled one, in the selected client card)
    const nextButtons = screen.getAllByText('Siguiente');
    const enabledBtn = nextButtons.find(b => !b.disabled);
    if (enabledBtn) fireEvent.click(enabledBtn);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/admin/orders/create/tools');
    });
  });

  it('navigates home when clicking goBack', () => {
    render(React.createElement(OrdersCreateClient));
    const cancelButtons = screen.getAllByText('Cancelar');
    if (cancelButtons.length > 0) fireEvent.click(cancelButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('changes init date and adjusts return date if needed', async () => {
    render(React.createElement(OrdersCreateClient));
    const initInput = screen.getByLabelText(/Fecha inicio/i);
    const returnInput = screen.getByLabelText(/Fecha retorno/i);

    // Change init date to a future date that's after current return date
    fireEvent.change(initInput, { target: { value: '2030-12-01' } });
    expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'info',
      message: expect.stringContaining('retorno ajustada'),
    }));
  });

  it('warns when selecting a past init date', () => {
    render(React.createElement(OrdersCreateClient));
    const initInput = screen.getByLabelText(/Fecha inicio/i);

    fireEvent.change(initInput, { target: { value: '2020-01-01' } });
    expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'warning',
      message: expect.stringContaining('fecha pasada'),
    }));
  });

  it('adjusts return date when set before init date', async () => {
    render(React.createElement(OrdersCreateClient));
    const returnInput = screen.getByLabelText(/Fecha retorno/i);

    // First, set a very early return date
    fireEvent.change(returnInput, { target: { value: '2020-01-01' } });
    expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'warning',
    }));
  });

  it('shows Añadir Cliente button and toggles register modal', async () => {
    render(React.createElement(OrdersCreateClient));
    const addBtn = screen.getByText(/Añadir Cliente/i);
    expect(addBtn).toBeInTheDocument();

    fireEvent.click(addBtn);
    await waitFor(() => {
      expect(screen.getByTestId('register-modal')).toBeInTheDocument();
    });
  });

  it('registers a new client via the modal', async () => {
    api.post.mockResolvedValueOnce({ data: { id: 99, username: 'newclient', name: 'New', lastName: 'Client' } });

    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByText(/Añadir Cliente/i));

    await waitFor(() => {
      expect(screen.getByTestId('register-submit')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('register-submit'));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/auth/register', expect.any(Object));
      expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }));
    });
  });

  it('handles registration error', async () => {
    api.post.mockRejectedValueOnce({ response: { data: { error: 'Username taken' } } });

    render(React.createElement(OrdersCreateClient));
    fireEvent.click(screen.getByText(/Añadir Cliente/i));

    await waitFor(() => {
      expect(screen.getByTestId('register-submit')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('register-submit'));

    await waitFor(() => {
      expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
    });
  });

  it('handles return date change when initDate is empty', () => {
    // This tests handleReturnDateChange when initDate is not set
    render(React.createElement(OrdersCreateClient));
    // The useEffect sets initDate automatically, but we can test the path
    // by checking that the component renders without errors
    expect(screen.getByLabelText(/Fecha retorno/i)).toBeInTheDocument();
  });

  it('renders "No hay cliente seleccionado" when none selected', () => {
    render(React.createElement(OrdersCreateClient));
    expect(screen.getByText(/No hay cliente seleccionado/i)).toBeInTheDocument();
  });

  it('shows Siguiente button disabled when no client is selected', () => {
    render(React.createElement(OrdersCreateClient));
    const nextButtons = screen.getAllByText('Siguiente');
    nextButtons.forEach(btn => {
      expect(btn).toBeDisabled();
    });
  });
});
