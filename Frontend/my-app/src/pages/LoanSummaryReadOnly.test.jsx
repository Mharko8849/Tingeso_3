import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockNavigate = vi.fn();
const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('../services/http-common', () => ({ default: { get: (...a) => mockGet(...a), post: (...a) => mockPost(...a) } }));
vi.mock('react-router-dom', () => ({ useNavigate: () => mockNavigate }));
vi.mock('../components/Layout/NavBar', () => ({ default: () => React.createElement('nav', { 'data-testid': 'navbar' }) }));
vi.mock('../components/Loading/LoadingSpinner', () => ({ default: ({ message }) => React.createElement('div', null, message || 'Loading...') }));
vi.mock('../components/Common/BackButton', () => ({ default: ({ onClick }) => React.createElement('button', { onClick, 'data-testid': 'back' }, 'Back') }));
vi.mock('../components/Badges/Badge', () => ({ default: ({ title }) => React.createElement('span', { 'data-testid': 'badge' }, title) }));
vi.mock('../components/Badges/statusToBadge', () => ({ statusToBadgeVariant: () => 'primary' }));
vi.mock('../utils/validation', () => ({ formatDate: (d) => d || '—' }));

import LoanSummaryReadOnly from './LoanSummaryReadOnly';

describe('LoanSummaryReadOnly', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock globalThis.location.pathname
    Object.defineProperty(window, 'location', {
      value: { pathname: '/loans/summary/123' },
      writable: true,
    });
  });

  it('renders loading state initially', () => {
    mockGet.mockReturnValue(new Promise(() => {})); // never resolves
    render(<LoanSummaryReadOnly />);
    expect(screen.getByText('Cargando resumen...')).toBeInTheDocument();
  });

  it('renders loan details after loading', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/loantool/total/')) return Promise.resolve({ data: 10000 });
      if (url.includes('/loantool/loan/')) {
        return Promise.resolve({
          data: [
            { id: 1, idTool: { name: 'Drill', priceRent: 5000, imageUrl: 'drill.jpg' }, toolActivity: 'Repair', amount: 2 },
          ],
        });
      }
      // Must come AFTER /loantool/ checks because /api/loan/ is a substring of /api/loantool/
      if (url.match(/\/api\/loan\/[^/]+$/) || url.includes('/api/loan/123')) {
        return Promise.resolve({
          data: {
            id: 123, status: 'ACTIVO',
            initDate: '2025-01-01', returnDate: '2025-01-15',
            idUser: { name: 'John', lastName: 'Doe', username: 'john', rut: '12345678-9' },
          },
        });
      }
      return Promise.resolve({ data: null });
    });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => {
      expect(screen.getAllByText(/Pedido #123/).length).toBeGreaterThan(0);
    }, { timeout: 3000 });
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('shows not found when loan is null', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loantool/total')) return Promise.resolve({ data: 0 });
      if (url.includes('/api/loantool/loan')) return Promise.resolve({ data: [] });
      return Promise.reject(new Error('not found'));
    });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => {
      expect(screen.getByText('Pedido no encontrado.')).toBeInTheDocument();
    });
  });

  it('navigates back to loans on back button click', async () => {
    mockGet.mockImplementation(() => Promise.resolve({ data: null }));
    render(<LoanSummaryReadOnly />);
    await waitFor(() => screen.getByTestId('back'));
    fireEvent.click(screen.getByTestId('back'));
    expect(mockNavigate).toHaveBeenCalledWith('/loans');
  });

  it('shows force close button when ACTIVO and no items', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loan/123') && !url.includes('loantool')) {
        return Promise.resolve({ data: { id: 123, status: 'ACTIVO', initDate: '', returnDate: '', idUser: { name: 'A' } } });
      }
      if (url.includes('/api/loantool/loan/123')) return Promise.resolve({ data: [] });
      if (url.includes('/api/loantool/total/123')) return Promise.resolve({ data: 0 });
      return Promise.resolve({ data: null });
    });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => {
      expect(screen.getByText('Finalizar pedido manualmente')).toBeInTheDocument();
    });
  });

  it('handles force close action', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loan/123') && !url.includes('loantool')) {
        return Promise.resolve({ data: { id: 123, status: 'ACTIVO', initDate: '', returnDate: '', idUser: { name: 'A' } } });
      }
      if (url.includes('/api/loantool/loan/123')) return Promise.resolve({ data: [] });
      if (url.includes('/api/loantool/total/123')) return Promise.resolve({ data: 0 });
      return Promise.resolve({ data: null });
    });
    mockPost.mockResolvedValue({ data: 'ok' });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => screen.getByText('Finalizar pedido manualmente'));
    fireEvent.click(screen.getByText('Finalizar pedido manualmente'));
    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/api/loan/close/123');
    });
  });

  it('handles force close error', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loan/123') && !url.includes('loantool')) {
        return Promise.resolve({ data: { id: 123, status: 'ACTIVO', initDate: '', returnDate: '', idUser: { name: 'A' } } });
      }
      if (url.includes('/api/loantool/loan/123')) return Promise.resolve({ data: [] });
      if (url.includes('/api/loantool/total/123')) return Promise.resolve({ data: 0 });
      return Promise.resolve({ data: null });
    });
    mockPost.mockRejectedValue({ response: { data: 'Error closing' } });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => screen.getByText('Finalizar pedido manualmente'));
    fireEvent.click(screen.getByText('Finalizar pedido manualmente'));
    await waitFor(() => {
      expect(screen.getByText('Error closing')).toBeInTheDocument();
    });
  });

  it('renders tool item with image URL starting with http', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loan/123') && !url.includes('loantool')) {
        return Promise.resolve({ data: { id: 123, status: 'CERRADO', initDate: '', returnDate: '', idUser: { name: 'Test' } } });
      }
      if (url.includes('/api/loantool/loan/123')) {
        return Promise.resolve({
          data: [{ id: 1, idTool: { toolName: 'Saw', imageUrl: 'http://example.com/saw.jpg', price: 3000 }, amount: 1 }],
        });
      }
      if (url.includes('/api/loantool/total/123')) return Promise.resolve({ data: 3000 });
      return Promise.resolve({ data: null });
    });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => {
      expect(screen.getByText('Saw')).toBeInTheDocument();
    });
  });

  it('shows empty tools message when no items', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/loan/123') && !url.includes('loantool')) {
        return Promise.resolve({ data: { id: 123, status: 'CERRADO', initDate: '', returnDate: '', idUser: { username: 'u' } } });
      }
      if (url.includes('/api/loantool/loan/123')) return Promise.resolve({ data: [] });
      if (url.includes('/api/loantool/total/123')) return Promise.resolve({ data: 0 });
      return Promise.resolve({ data: null });
    });
    render(<LoanSummaryReadOnly />);
    await waitFor(() => {
      expect(screen.getByText('No hay herramientas en este pedido.')).toBeInTheDocument();
    });
  });
});
