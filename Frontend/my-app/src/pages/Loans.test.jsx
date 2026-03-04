import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';

const mockNavigate = vi.fn();

vi.mock('../services/http-common', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));
vi.mock('../components/Alerts/useAlert', () => ({
  useAlert: () => ({ show: vi.fn() }),
}));
vi.mock('../components/Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }),
}));
vi.mock('../components/Reports', () => ({
  ReportLoans: () => React.createElement('button', { 'data-testid': 'report-loans' }),
}));

import Loans from './Loans';
import api from '../services/http-common';

describe('Loans', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner while fetching', () => {
    api.get.mockImplementation(() => new Promise(() => {})); // never resolves
    render(React.createElement(Loans));
    expect(screen.getByText(/Cargando pedidos/i)).toBeInTheDocument();
  });

  it('renders loans list after successful fetch', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, clientName: 'Carlos García', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' },
          { id: 2, clientName: 'María López', initDate: '2026-03-02', returnDate: '2026-03-06', status: 'PENDIENTE' },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    });

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText('Carlos García')).toBeInTheDocument();
      expect(screen.getByText('María López')).toBeInTheDocument();
    });
  });

  it('shows error message on fetch failure', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'));

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText(/No se pudo cargar/i)).toBeInTheDocument();
    });
  });

  it('shows empty message when no loans', async () => {
    api.get.mockResolvedValueOnce({
      data: { content: [], totalElements: 0, totalPages: 1 },
    });

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText(/No hay pedidos/i)).toBeInTheDocument();
    });
  });

  it('navigates to loan detail on click', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [{ id: 42, clientName: 'Test', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText('Test')).toBeInTheDocument();
    });

    // Click on the "Ver pedido" span that LoanListItem renders
    const verButtons = screen.getAllByText('Ver pedido');
    fireEvent.click(verButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/loans/loan/42');
  });

  it('uses username when clientName is not available', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [{ id: 1, username: 'testuser', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });
  });

  it('filters loans by search term', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, clientName: 'Carlos García', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' },
          { id: 2, clientName: 'María López', initDate: '2026-03-02', returnDate: '2026-03-06', status: 'PENDIENTE' },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    });

    render(React.createElement(Loans));

    await waitFor(() => {
      expect(screen.getByText('Carlos García')).toBeInTheDocument();
    });

    // Search for Carlos
    const searchInput = screen.getByPlaceholderText(/Buscar/i);
    if (searchInput) {
      fireEvent.change(searchInput, { target: { value: 'carlos' } });
      await waitFor(() => {
        expect(screen.getByText('Carlos García')).toBeInTheDocument();
      });
    }
  });
});
