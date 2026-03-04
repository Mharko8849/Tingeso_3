import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../Common/csvUtils', () => ({
  buildCsv: vi.fn(() => 'csv-data'),
  downloadBlob: vi.fn(),
}));

vi.mock('../Alerts/useAlert', () => ({
  useAlert: () => ({ show: vi.fn() }),
}));

import ReportKardex from './ReportKardex';
import { buildCsv, downloadBlob } from '../Common/csvUtils';

describe('ReportKardex', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('renders the download button', () => {
    render(<ReportKardex rows={[]} />);
    expect(screen.getByText(/Generar Reporte/)).toBeInTheDocument();
  });

  it('downloads CSV with default filename', () => {
    render(<ReportKardex rows={[]} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    expect(downloadBlob).toHaveBeenCalled();
    const fname = downloadBlob.mock.calls[0][1];
    expect(fname).toMatch(/^kardex_\d{4}-\d{2}-\d{2}\.csv$/);
  });

  it('uses custom filename when provided', () => {
    render(<ReportKardex rows={[]} filename="my_report.csv" />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    expect(downloadBlob).toHaveBeenCalledWith('csv-data', 'my_report.csv');
  });

  it('maps movement rows correctly', () => {
    const rows = [{
      date: '2025-03-01T10:00:00Z',
      idEmployee: { id: 1, name: 'Admin' },
      idTool: { id: 5, toolName: 'Drill' },
      user: { id: 10, name: 'Client' },
      type: 'PRESTAMO',
      qty: 2,
      cost: 5000,
    }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][1]).toBe('1 - Admin');
    expect(mapped[0][2]).toBe('5 - Drill');
    expect(mapped[0][3]).toBe('10 - Client');
    expect(mapped[0][4]).toBe('PRESTAMO');
    expect(mapped[0][5]).toBe(2);
    expect(mapped[0][6]).toBe(5000);
  });

  it('renderUser handles null → "—"', () => {
    const rows = [{ date: '2025-01-01', idEmployee: null, idTool: null, user: null, type: '' }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][1]).toBe('—');
    expect(mapped[0][2]).toBe('—');
    expect(mapped[0][3]).toBe('—');
  });

  it('renderUser handles user with _id only', () => {
    const rows = [{ date: '', employee: { _id: 99 }, tool: { _id: 88 }, client: { _id: 77 }, type: 'DEVOLUCION' }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][1]).toBe('99');
    expect(mapped[0][2]).toBe('88');
    expect(mapped[0][3]).toBe('77');
  });

  it('renderUser handles user with no id → name', () => {
    const rows = [{ date: '', employee: { name: 'OnlyName' }, tool: { name: 'OnlyTool' }, user: 'stringUser', type: '' }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][1]).toBe('OnlyName');
    expect(mapped[0][2]).toBe('OnlyTool');
    expect(mapped[0][3]).toBe('stringUser');
  });

  it('renderUser handles object with no id or name → JSON.stringify', () => {
    const rows = [{ date: '', employeeId: { foo: 'bar' }, idTool: { baz: 1 }, user: null, type: '' }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][1]).toBe('{"foo":"bar"}');
    expect(mapped[0][2]).toBe('{"baz":1}');
  });

  it('handles alternative field names for quantity and amount', () => {
    const rows = [{ date: '', type: '', quantity: 10, amount: 2000 }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][5]).toBe(10);
    expect(mapped[0][6]).toBe(2000);
  });

  it('handles fallback quantity/amount fields', () => {
    const rows = [{ date: '', type: '', cantidad: 3, balance: 999 }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][5]).toBe(3);
    expect(mapped[0][6]).toBe(999);
  });

  it('handles missing quantity/amount → "—"', () => {
    const rows = [{ date: '', type: '' }];
    render(<ReportKardex rows={rows} />);
    fireEvent.click(screen.getByText(/Generar Reporte/));
    const mapped = buildCsv.mock.calls[0][1];
    expect(mapped[0][5]).toBe('—');
    expect(mapped[0][6]).toBe('—');
  });
});
