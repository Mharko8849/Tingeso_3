import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';

const mockShow = vi.fn();

vi.mock('../Alerts/useAlert', () => ({
  useAlert: () => ({ show: mockShow }),
}));
vi.mock('../Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('../Common/csvUtils', () => ({
  buildCsv: vi.fn(() => 'csv-content'),
  downloadBlob: vi.fn(),
}));

import ReportLoans from './ReportLoans';
import { buildCsv, downloadBlob } from '../Common/csvUtils';

describe('ReportLoans', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the download button', () => {
    render(React.createElement(ReportLoans));
    expect(screen.getByText(/Generar reporte/i)).toBeInTheDocument();
  });

  it('generates CSV with correct headers and data', () => {
    const rows = [
      { id: 1, client: { name: 'Carlos', lastName: 'García' }, initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' },
      { id: 2, idUser: { username: 'maria' }, initDate: '2026-03-02', returnDate: '2026-03-06', status: 'PENDIENTE' },
    ];

    render(React.createElement(ReportLoans, { rows }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(buildCsv).toHaveBeenCalledWith(
      expect.arrayContaining(['Pedido #', 'Cliente']),
      expect.any(Array),
    );
    expect(downloadBlob).toHaveBeenCalled();
    expect(mockShow).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }));
  });

  it('handles client as string', () => {
    const rows = [{ id: 1, user: 'ClientString', initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }];

    render(React.createElement(ReportLoans, { rows }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(buildCsv).toHaveBeenCalled();
  });

  it('handles missing client', () => {
    const rows = [{ id: 1, initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }];

    render(React.createElement(ReportLoans, { rows }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(buildCsv).toHaveBeenCalled();
  });

  it('handles object client without name', () => {
    const rows = [
      { id: 1, client: { username: 'user1' }, initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' },
      { id: 2, client: { email: 'a@b.com' }, initDate: '2026-03-02', returnDate: '2026-03-06', status: 'PENDIENTE' },
    ];

    render(React.createElement(ReportLoans, { rows }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(buildCsv).toHaveBeenCalled();
  });

  it('uses custom filename when provided', () => {
    const rows = [{ id: 1, initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }];

    render(React.createElement(ReportLoans, { rows, filename: 'custom.csv' }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(downloadBlob).toHaveBeenCalledWith('csv-content', 'custom.csv');
  });

  it('generates default filename when not provided', () => {
    const rows = [{ id: 1, initDate: '2026-03-01', returnDate: '2026-03-05', status: 'ACTIVO' }];

    render(React.createElement(ReportLoans, { rows }));
    fireEvent.click(screen.getByText(/Generar reporte/i));

    expect(downloadBlob).toHaveBeenCalledWith('csv-content', expect.stringContaining('reporte_pedidos_'));
  });
});
