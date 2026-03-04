import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.fn();
const mockPost = vi.fn();
vi.mock('../../services/http-common', () => ({ default: { get: (...a) => mockGet(...a), post: (...a) => mockPost(...a) } }));
vi.mock('../../components/Alerts/useAlert', () => ({
  useAlert: () => ({ show: vi.fn() }),
}));

import DebtPaymentModal from './DebtPaymentModal';

describe('DebtPaymentModal', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('returns null when open is false', () => {
    const { container } = render(<DebtPaymentModal open={false} loan={{ id: 1 }} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders modal title when open', async () => {
    mockGet.mockResolvedValue({ data: [] });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 42 }} totalFine={5000} />);
    expect(screen.getByText(/Pagar Multa - Pedido #42/)).toBeInTheDocument();
  });

  it('shows loading message then empty state', async () => {
    mockGet.mockResolvedValue({ data: [] });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 1 }} totalFine={0} />);
    await waitFor(() => {
      expect(screen.getByText(/No hay herramientas con multa/)).toBeInTheDocument();
    });
  });

  it('renders items with fines', async () => {
    mockGet.mockResolvedValue({
      data: [
        { id: 10, idTool: { toolName: 'Drill', imageUrl: 'drill.jpg' }, fine: 3000 },
        { id: 11, idTool: { name: 'Saw' }, debt: 2000 },
      ],
    });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 1 }} totalFine={5000} onPaid={vi.fn()} />);
    await waitFor(() => {
      expect(screen.getByText('Drill')).toBeInTheDocument();
      expect(screen.getByText('Saw')).toBeInTheDocument();
    });
  });

  it('calls close button', async () => {
    mockGet.mockResolvedValue({ data: [] });
    const onClose = vi.fn();
    render(<DebtPaymentModal open={true} onClose={onClose} loan={{ id: 1 }} totalFine={0} />);
    await waitFor(() => screen.getByText('✕'));
    screen.getByText('✕').click();
    expect(onClose).toHaveBeenCalled();
  });

  it('submits payment successfully', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/loantool/loan/')) return Promise.resolve({ data: [{ id: 1, fine: 100, idTool: { toolName: 'X' } }] });
      if (url.includes('/user/me')) return Promise.resolve({ data: { id: 55 } });
      return Promise.resolve({ data: {} });
    });
    mockPost.mockResolvedValue({ data: 'ok' });
    const onPaid = vi.fn();
    const onClose = vi.fn();
    render(<DebtPaymentModal open={true} onClose={onClose} loan={{ id: 1 }} totalFine={100} onPaid={onPaid} />);
    await waitFor(() => screen.getByText('Pagar deuda'));
    screen.getByText('Pagar deuda').click();
    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/api/loantool/paydebt/1/user/55');
      expect(onPaid).toHaveBeenCalled();
    });
  });

  it('handles payment error', async () => {
    mockGet.mockImplementation((url) => {
      if (url.includes('/loantool/loan/')) return Promise.resolve({ data: [{ id: 1, fine: 100, idTool: { name: 'X' } }] });
      if (url.includes('/user/me')) return Promise.resolve({ data: { id: 55 } });
      return Promise.resolve({ data: {} });
    });
    mockPost.mockRejectedValue({ response: { data: 'Payment error' } });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 1 }} totalFine={100} onPaid={vi.fn()} />);
    await waitFor(() => screen.getByText('Pagar deuda'));
    screen.getByText('Pagar deuda').click();
    // Should not throw, error handled internally
    await waitFor(() => expect(mockPost).toHaveBeenCalled());
  });

  it('renders item without image', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1, idTool: { toolName: 'NoImg' }, fine: 100 }] });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 1 }} totalFine={100} />);
    await waitFor(() => {
      expect(screen.getByText('NoImg')).toBeInTheDocument();
      expect(screen.getByText('Sin imagen')).toBeInTheDocument();
    });
  });

  it('renders item with http image URL', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1, idTool: { toolName: 'HttpImg', imageUrl: 'http://example.com/img.jpg' }, fine: 50 }] });
    render(<DebtPaymentModal open={true} onClose={vi.fn()} loan={{ id: 1 }} totalFine={50} />);
    await waitFor(() => {
      expect(screen.getByAltText('HttpImg')).toHaveAttribute('src', 'http://example.com/img.jpg');
    });
  });
});
