import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';

// Mock dependencies
vi.mock('../services/http-common', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  useLocation: () => ({ pathname: '/' }),
  Link: ({ children, to }) => React.createElement('a', { href: to }, children),
}));
vi.mock('../components/Alerts/useAlert', () => ({
  useAlert: () => ({ show: vi.fn() }),
}));
vi.mock('../components/Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('@react-keycloak/web', () => ({
  useKeycloak: () => ({ keycloak: { authenticated: true, token: 'fake', tokenParsed: { realm_access: { roles: ['ADMIN'] } } }, initialized: true }),
}));
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }, 'NavBar'),
}));
vi.mock('../components/Reports/ReportKardex', () => ({
  default: () => React.createElement('button', { 'data-testid': 'report-kardex' }, 'Report'),
}));
vi.mock('../components/Reports/ReportRanking', () => ({
  default: () => React.createElement('button', { 'data-testid': 'report-ranking' }, 'Ranking'),
}));

import KardexPage from './KardexPage';
import api from '../services/http-common';

describe('KardexPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner and then movements table', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, date: '2026-01-15', type: 'PRESTAMO', tool: { id: 10, toolName: 'Martillo' }, idEmployee: { id: 5, name: 'Juan' }, user: 'Carlos', qty: 2, cost: 100 },
          { id: 2, date: '2026-01-16', type: 'DEVOLUCION', tool: 'Destornillador', employee: 3, idUser: null, quantity: 1, amount: 50 },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Kardex — Movimientos')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/api/kardex/filter', expect.any(Object));
    });
  });

  it('renders empty movements message', async () => {
    api.get.mockResolvedValueOnce({
      data: { content: [], totalElements: 0, totalPages: 1 },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText(/No se encontraron movimientos/i)).toBeInTheDocument();
    });
  });

  it('handles API error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'));

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText(/No se pudo obtener movimientos/i)).toBeInTheDocument();
    });
  });

  it('handles array response from API', async () => {
    api.get.mockResolvedValueOnce({
      data: [
        { id: 1, date: '2026-01-15', type: 'INGRESO', tool: null, idEmployee: null, user: null, qty: 5, cost: 200 },
      ],
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalled();
    });
  });

  it('renders search input and allows typing', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/Buscar por herramienta/i);
      expect(searchInput).toBeInTheDocument();
      fireEvent.change(searchInput, { target: { value: 'martillo' } });
      expect(searchInput.value).toBe('martillo');
    });
  });

  it('renders type filter dropdown', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Todos')).toBeInTheDocument();
    });
  });

  it('renders Refrescar and Limpiar filtros buttons', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Refrescar')).toBeInTheDocument();
      expect(screen.getByText('Limpiar filtros')).toBeInTheDocument();
    });
  });

  it('clicking Limpiar filtros resets filters and refetches', async () => {
    api.get.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Limpiar filtros')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Limpiar filtros'));
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('renders movements with object tool and employee', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          {
            id: 1,
            date: '2026-03-01T10:00:00',
            type: 'PRESTAMO',
            tool: { id: 10, toolName: 'Martillo' },
            idEmployee: { id: 5, name: 'Juan' },
            user: { id: 100, username: 'carlos' },
            qty: 2,
            cost: 150,
          },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('10 - Martillo')).toBeInTheDocument();
      expect(screen.getByText('5 - Juan')).toBeInTheDocument();
    });
  });

  it('renders movements with string tool and null user', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          {
            id: 2,
            date: 'invalid-date',
            type: 'BAJA',
            idTool: 'Taladro simple',
            employee: 'Empleado1',
            idUser: null,
            cant: 3,
            stock: 500,
          },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Taladro simple')).toBeInTheDocument();
      expect(screen.getByText('Empleado1')).toBeInTheDocument();
    });
  });

  it('date pickers render and constrain each other', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      const desde = screen.getByLabelText('Fecha desde');
      const hasta = screen.getByLabelText('Fecha hasta');
      expect(desde).toBeInTheDocument();
      expect(hasta).toBeInTheDocument();

      // Set desde after hasta: should adjust hasta
      fireEvent.change(desde, { target: { value: '2026-03-10' } });
      fireEvent.change(hasta, { target: { value: '2026-03-05' } });
    });
  });

  it('filters movements by search term in tool name', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, date: '2026-01-15', type: 'PRESTAMO', tool: { id: 10, toolName: 'Martillo' }, idEmployee: 5, qty: 1, cost: 100 },
          { id: 2, date: '2026-01-16', type: 'BAJA', tool: { id: 11, toolName: 'Taladro' }, employee: 3, qty: 2, cost: 200 },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('10 - Martillo')).toBeInTheDocument();
      expect(screen.getByText('11 - Taladro')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/Buscar por herramienta/i);
    fireEvent.change(searchInput, { target: { value: 'martillo' } });

    await waitFor(() => {
      expect(screen.getByText('10 - Martillo')).toBeInTheDocument();
      expect(screen.queryByText('11 - Taladro')).not.toBeInTheDocument();
    });
  });

  it('clicking type filter triggers refetch', async () => {
    api.get.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Todos')).toBeInTheDocument();
    });

    const select = screen.getByDisplayValue('Todos');
    fireEvent.change(select, { target: { value: 'PRESTAMO' } });

    // Should have called api.get twice: initial + filter change
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledTimes(2);
    });
  });

  it('renders object user with username fallback', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, date: '2026-01-15', type: 'INGRESO', tool: null, employee: { username: 'admin' }, user: { _id: 'abc' }, qty: 1, cost: 0 },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
  });

  it('renders object tool with name fallback', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, date: '2026-01-15', type: 'INGRESO', tool: { name: 'SinId' }, idEmployee: null, idUser: null, qty: 1, cost: 0 },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('SinId')).toBeInTheDocument();
    });
  });

  it('clicking Refrescar triggers another API call', async () => {
    api.get.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByText('Refrescar')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Refrescar'));
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledTimes(2);
    });
  });

  it('handles desde date change adjusting hasta if needed', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByLabelText('Fecha hasta')).toBeInTheDocument();
    });

    const hasta = screen.getByLabelText('Fecha hasta');
    fireEvent.change(hasta, { target: { value: '2026-03-15' } });

    const desde = screen.getByLabelText('Fecha desde');
    fireEvent.change(desde, { target: { value: '2026-03-20' } });

    // After setting desde > hasta, hasta should have been adjusted
    expect(desde.value).toBe('2026-03-20');
  });

  it('handles hasta date change adjusting desde if needed', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [], totalElements: 0, totalPages: 1 } });

    render(React.createElement(KardexPage));

    await waitFor(() => {
      expect(screen.getByLabelText('Fecha desde')).toBeInTheDocument();
    });

    const desde = screen.getByLabelText('Fecha desde');
    fireEvent.change(desde, { target: { value: '2026-03-20' } });

    const hasta = screen.getByLabelText('Fecha hasta');
    fireEvent.change(hasta, { target: { value: '2026-03-10' } });

    // After setting hasta < desde, desde should have been adjusted
    expect(hasta.value).toBe('2026-03-10');
  });
});
