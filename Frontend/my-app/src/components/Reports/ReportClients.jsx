import React from 'react';
import PropTypes from 'prop-types';
import { buildCsv, downloadBlob } from '../Common/csvUtils';
import { useAlert } from '../Alerts/useAlert';

/**
 * Determines the display state of a client from various possible field names.
 */
const getClientState = (u) => {
  const candidates = [u.stateClient, u.state, u.state_client, u.status, u.enabled, u.active, u.isActive, u.estado];
  for (const v of candidates) {
    if (v === undefined || v === null || v === '') continue;
    if (typeof v === 'boolean') return v ? 'ACTIVO' : 'RESTRINGIDO';
    const vs = String(v).toUpperCase();
    if (vs === 'TRUE' || vs === 'ACTIVO') return 'ACTIVO';
    if (vs === 'FALSE' || vs === 'RESTRINGIDO') return 'RESTRINGIDO';
    return String(v);
  }
  return '—';
};

/**
 * Component that generates a CSV report for clients.
 */
const ReportClients = ({ rows = [], filename }) => {
  const { show } = useAlert();

  const downloadCSV = () => {
    const headers = ['ID', 'Username', 'Nombre', 'Apellido', 'Email', 'RUT', 'Pedidos', 'Estado', 'Rol'];
    const mapped = rows.map(c => [
      c.id ?? '',
      c.username ?? '',
      c.name ?? '',
      c.lastName ?? '',
      c.email ?? '',
      c.rut ?? c.RUT ?? '—',
      c.loans != null ? c.loans : 0,
      getClientState(c),
      c.rol ?? '',
    ]);
    const name = filename || `reporte_clientes_${new Date().toISOString().slice(0, 10)}.csv`;
    downloadBlob(buildCsv(headers, mapped), name);
    show({ severity: 'success', message: 'Reporte de clientes generado correctamente' });
  };

  return (
    <button onClick={downloadCSV} className="primary-cta" type="button">Generar reporte (CSV)</button>
  );
};

ReportClients.propTypes = {
  filename: PropTypes.string,
  rows: PropTypes.array,
};

export default ReportClients;
