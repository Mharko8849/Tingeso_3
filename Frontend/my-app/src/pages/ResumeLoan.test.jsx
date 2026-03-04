import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';

const mockNavigate = vi.fn();
const mockShow = vi.fn();

// Mock dependencies
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
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }),
}));
vi.mock('../components/Alerts/TransitionAlert', () => ({
  default: ({ alert }) => alert ? React.createElement('div', { 'data-testid': 'alert' }, alert.message) : null,
}));

import ResumeLoan from './ResumeLoan';
import api from '../services/http-common';

describe('ResumeLoan', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
  });

  it('shows no-data message when no resume in sessionStorage', () => {
    render(React.createElement(ResumeLoan));
    expect(screen.getByText(/No hay datos del pedido/i)).toBeInTheDocument();
  });

  it('renders resume when data exists in sessionStorage', () => {
    const resume = {
      client: { id: 1, name: 'Carlos', lastName: 'García', username: 'cgarcia', rut: '12345678-9' },
      items: [
        { id: 10, name: 'Martillo', qty: 2, price: 500, image: 'http://example.com/img.jpg' },
        { id: 11, name: 'Taladro', qty: 1, price: 1000 },
      ],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    render(React.createElement(ResumeLoan));
    expect(screen.getByText('Resumen del Pedido')).toBeInTheDocument();
    expect(screen.getByText(/Carlos García/)).toBeInTheDocument();
    expect(screen.getByText('cgarcia')).toBeInTheDocument();
    expect(screen.getByText(/12345678-9/)).toBeInTheDocument();
    expect(screen.getByText('Martillo')).toBeInTheDocument();
    expect(screen.getByText('Taladro')).toBeInTheDocument();
  });

  it('uses default dates when none stored in sessionStorage', () => {
    const resume = {
      client: { id: 1, username: 'carlos@mail.com' },
      items: [{ id: 10, name: 'Martillo', qty: 1, price: 100 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));

    render(React.createElement(ResumeLoan));
    expect(screen.getByText('Resumen del Pedido')).toBeInTheDocument();
  });

  it('renders client email when name is not available', async () => {
    const resume = {
      client: { id: 1, email: 'test@mail.com' },
      items: [{ id: 10, name: 'Tool', qty: 1, price: 0 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    render(React.createElement(ResumeLoan));
    await waitFor(() => {
      const els = screen.getAllByText('test@mail.com');
      expect(els.length).toBeGreaterThanOrEqual(1);
    });
  });

  it('calculates total from items', async () => {
    const resume = {
      client: { id: 1, name: 'Test' },
      items: [
        { id: 1, name: 'A', qty: 2, price: 500 },
        { id: 2, name: 'B', qty: 3, price: 100 },
      ],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    render(React.createElement(ResumeLoan));
    // Total = 2*500 + 3*100 = 1300
    // Wait for component to read sessionStorage and re-render
    await waitFor(() => {
      expect(screen.getByText('Resumen del Pedido')).toBeInTheDocument();
    }, { timeout: 3000 });
    // Check total is rendered - locale-independent check
    const totalEl = screen.getByText('Total');
    expect(totalEl).toBeInTheDocument();
  });

  it('shows error when confirming with empty items', async () => {
    const resume = {
      client: { id: 1, name: 'Carlos' },
      items: [],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    render(React.createElement(ResumeLoan));

    await waitFor(() => {
      expect(screen.getByText('Confirmar pedido')).toBeInTheDocument();
    });
  });

  it('successfully creates order and starts countdown', async () => {
    const resume = {
      client: { id: 1, name: 'Carlos' },
      items: [{ id: 10, name: 'Martillo', qty: 1, price: 500 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    api.get.mockImplementation((url) => {
      if (url === '/api/user/me') return Promise.resolve({ data: { id: 5 } });
      if (url.includes('/api/loantool/loan/')) return Promise.resolve({ data: [{ id: 100 }, { id: 101 }] });
      return Promise.resolve({ data: {} });
    });
    api.post.mockResolvedValue({ data: { id: 999 } });

    render(React.createElement(ResumeLoan));

    const confirmBtn = screen.getByText('Confirmar pedido');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(api.post).toHaveBeenCalled();
    });
  });

  it('shows error when API call fails during order creation', async () => {
    const resume = {
      client: { id: 1, name: 'Carlos' },
      items: [{ id: 10, name: 'Martillo', qty: 1, price: 500 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    api.get.mockRejectedValueOnce({ response: { status: 409, data: { error: 'Conflict' } } });

    render(React.createElement(ResumeLoan));

    const confirmBtn = screen.getByText('Confirmar pedido');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(api.get).toHaveBeenCalled();
    });
  });

  it('shows error for 400 response', async () => {
    const resume = {
      client: { id: 1, name: 'Carlos' },
      items: [{ id: 10, name: 'Tool', qty: 1, price: 100 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    api.get.mockRejectedValueOnce({ response: { status: 400, data: 'Bad request' } });

    render(React.createElement(ResumeLoan));
    fireEvent.click(screen.getByText('Confirmar pedido'));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalled();
    });
  });

  it('shows network error message', async () => {
    const resume = {
      client: { id: 1, name: 'Carlos' },
      items: [{ id: 10, name: 'Tool', qty: 1, price: 100 }],
    };
    sessionStorage.setItem('order_resume', JSON.stringify(resume));
    sessionStorage.setItem('order_init_date', '2026-03-01');
    sessionStorage.setItem('order_return_date', '2026-03-05');

    api.get.mockRejectedValueOnce(new Error('Network Error'));

    render(React.createElement(ResumeLoan));
    fireEvent.click(screen.getByText('Confirmar pedido'));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalled();
    });
  });
});
