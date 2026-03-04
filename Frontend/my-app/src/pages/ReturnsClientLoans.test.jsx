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
vi.mock('../components/Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }),
}));

import ReturnsClientLoans from './ReturnsClientLoans';
import api from '../services/http-common';

// Mock globalThis.location.pathname
const originalLocation = globalThis.location;
beforeEach(() => {
  delete globalThis.location;
  globalThis.location = { ...originalLocation, pathname: '/admin/returns/client/123' };
});
afterAll(() => {
  globalThis.location = originalLocation;
});

describe('ReturnsClientLoans', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner while fetching', () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    render(React.createElement(ReturnsClientLoans));
    expect(screen.getByText(/Cargando pedidos/i)).toBeInTheDocument();
  });

  it('renders client loans after successful fetch', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [
          { id: 1, clientName: 'Carlos', username: 'cgarcia', clientEmail: 'c@mail.com', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(ReturnsClientLoans));

    await waitFor(() => {
      expect(screen.getByText(/Pedidos del cliente Carlos/i)).toBeInTheDocument();
    });
  });

  it('shows empty message when client has no loans', async () => {
    api.get.mockResolvedValueOnce({
      data: { content: [], totalElements: 0, totalPages: 0 },
    });

    render(React.createElement(ReturnsClientLoans));

    await waitFor(() => {
      expect(screen.getByText(/no tiene pedidos/i)).toBeInTheDocument();
    });
  });

  it('handles API error', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'));

    render(React.createElement(ReturnsClientLoans));

    await waitFor(() => {
      expect(screen.getByText(/no tiene pedidos/i)).toBeInTheDocument();
    });
  });

  it('navigates to loan on click', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        content: [{ id: 99, clientName: 'Test', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }],
        totalElements: 1,
        totalPages: 1,
      },
    });

    render(React.createElement(ReturnsClientLoans));

    await waitFor(() => {
      const verButtons = screen.getAllByText('Ver pedido');
      fireEvent.click(verButtons[0]);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/returns/loan/99');
    });
  });
});
