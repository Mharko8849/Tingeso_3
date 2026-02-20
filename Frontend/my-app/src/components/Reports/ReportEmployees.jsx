import React from 'react';
import { buildCsv, downloadBlob } from '../Common/csvUtils';
import { useAlert } from '../Alerts/useAlert';

/**
 * Component that generates a CSV report for employees.
 * It receives a list of employee objects and a filename.
 *
 * Input: props (rows, filename)
 * Output: JSX Element (button)
 */
const ReportEmployees = ({ rows = [], filename }) => {
  const { show } = useAlert();
  /**
   * Generates the CSV content and triggers the download.
   */
  const downloadCSV = () => {
    const headers = [ 'ID','Nombre', 'Apellido', 'Email', 'Rol'];

    const mapped = rows.map(u => [
      u.id != null ? String(u.id) : (u.username || ''),
      u.name || '',
      u.lastName || '',
      u.email || '',
      u.rol || u.role || ''
    ]);

    const csv = buildCsv(headers, mapped);
    const name = filename || `reporte_empleados_${new Date().toISOString().slice(0,10)}.csv`;
    downloadBlob(csv, name);
    show({ severity: 'success', message: 'Reporte de empleados generado correctamente' });
  };

  return (
    <button onClick={downloadCSV} className="primary-cta" type="button">Generar reporte (CSV)</button>
  );
};

export default ReportEmployees;
