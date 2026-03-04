import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

vi.mock('../Common/csvUtils', () => ({
  buildCsv: vi.fn(() => 'csv-data'),
  downloadBlob: vi.fn(),
}));

vi.mock('../Alerts/useAlert', () => ({
  useAlert: () => ({ show: vi.fn() }),
}));

import ReportClients from './ReportClients';
import { buildCsv, downloadBlob } from '../Common/csvUtils';

describe('ReportClients', () => {
  it('renders the download button', () => {
    render(<ReportClients rows={[]} />);
    expect(screen.getByText('Generar reporte (CSV)')).toBeInTheDocument();
  });

  it('downloads CSV when clicked', () => {
    const rows = [
      { id: 1, username: 'user1', name: 'John', lastName: 'Doe', email: 'john@test.com', rut: '12345678-9', loans: 5, stateClient: true, rol: 'CLIENT' },
    ];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    expect(buildCsv).toHaveBeenCalled();
    expect(downloadBlob).toHaveBeenCalled();
  });

  it('uses custom filename', () => {
    render(<ReportClients rows={[]} filename="custom.csv" />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    expect(downloadBlob).toHaveBeenCalledWith('csv-data', 'custom.csv');
  });

  it('handles getClientState with boolean false', () => {
    const rows = [{ id: 1, stateClient: false }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    expect(buildCsv).toHaveBeenCalled();
    // The mapped row should contain 'RESTRINGIDO' for false state
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    const mapped = call[1];
    expect(mapped[0]).toContain('RESTRINGIDO');
  });

  it('handles getClientState with string "ACTIVO"', () => {
    const rows = [{ id: 2, state: 'ACTIVO' }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('ACTIVO');
  });

  it('handles getClientState with string "RESTRINGIDO"', () => {
    const rows = [{ id: 3, status: 'RESTRINGIDO' }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('RESTRINGIDO');
  });

  it('handles getClientState with boolean true', () => {
    const rows = [{ id: 4, enabled: true }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('ACTIVO');
  });

  it('handles getClientState with no state fields → "—"', () => {
    const rows = [{ id: 5 }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('—');
  });

  it('handles getClientState with string "TRUE"', () => {
    const rows = [{ id: 6, active: 'TRUE' }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('ACTIVO');
  });

  it('handles getClientState with arbitrary string', () => {
    const rows = [{ id: 7, estado: 'SUSPENDIDO' }];
    render(<ReportClients rows={rows} />);
    fireEvent.click(screen.getByText('Generar reporte (CSV)'));
    const call = buildCsv.mock.calls[buildCsv.mock.calls.length - 1];
    expect(call[1][0]).toContain('SUSPENDIDO');
  });
});
