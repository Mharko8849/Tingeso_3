import React from 'react';
import { buildCsv, downloadBlob } from '../Common/csvUtils';
import { useAlert } from '../Alerts/useAlert';

/**
 * Component that generates a CSV report for loans (orders).
 * It receives a list of loan objects and a filename.
 *
 * Input: props (rows, filename)
 * Output: JSX Element (button)
 */
const ReportLoans = ({ rows = [], filename }) => {
  const { show } = useAlert();
  /**
   * Generates the CSV content and triggers the download.
   */
  const downloadCSV = () => {
    const headers = ['Pedido #', 'Cliente', 'Fecha inicio', 'Fecha devolución', 'Fecha Actual', 'Estado'];
    const today = new Date().toISOString().slice(0, 10);

    const mapped = rows.map(l => {
      // Handle different client field names: client, idUser, user
      const clientObj = l.client || l.idUser || l.user;
      let clientName = '—';
      
      if (clientObj) {
        if (typeof clientObj === 'object') {
          if (clientObj.name) {
            clientName = `${clientObj.name} ${clientObj.lastName || ''}`.trim();
          } else {
            clientName = clientObj.username || clientObj.email || '—';
          }
        } else {
          clientName = String(clientObj);
        }
      }

      return [
        l.id ?? '',
        clientName,
        l.initDate ?? '',
        l.returnDate ?? '',
        today,
        l.status ?? ''
      ];
    });

    const csv = buildCsv(headers, mapped);
    const name = filename || `reporte_pedidos_${new Date().toISOString().slice(0,10)}.csv`;
    downloadBlob(csv, name);
    show({ severity: 'success', message: 'Reporte de pedidos generado correctamente' });
  };

  return (
    <button onClick={downloadCSV} className="primary-cta" type="button">Generar reporte (CSV)</button>
  );
};

export default ReportLoans;
