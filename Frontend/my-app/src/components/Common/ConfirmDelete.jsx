import React from 'react';
import PropTypes from 'prop-types';

/**
 * A modal component to confirm the deletion of an employee.
 *
 * @param {Object} props - The component props.
 * @param {boolean} [props.open=false] - Whether the modal is open.
 * @param {Object} [props.employee=null] - The employee object to be deleted.
 * @param {Function} [props.onConfirm] - Callback when deletion is confirmed.
 * @param {Function} [props.onCancel] - Callback when deletion is cancelled.
 * @returns {JSX.Element|null} - The confirmation modal or null if not open.
 */
const ConfirmDelete = ({ open = false, employee = null, onConfirm = () => {}, onCancel = () => {} }) => {
  if (!open) return null;

  const name = employee ? `${employee.name || ''}` : '';
  const lastName = employee ? `${employee.lastName || ''}` : '';
  const rol = employee ? `${employee.rol || ''}` : '';

  return (
    <div className="tool-overlay" >
      <button type="button" onClick={onCancel} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
      <div className="tool-content" style={{ maxWidth: 520 }}>
        <button className="close-btn" onClick={onCancel} aria-label="Cerrar">✕</button>
        <h3 style={{ marginTop: 0 }}>Confirmar eliminación</h3>
        <p>Estás a punto de eliminar a:</p>
        <div style={{ fontWeight: 600, marginBottom: 12 }}>{name} {lastName} — <span style={{ fontWeight: 500 }}>{rol}</span></div>
        <p>¿Estás seguro que deseas continuar? Esta acción no se puede deshacer.</p>
        <div style={{ display: 'flex', gap: 8, width: '100%', marginTop: 12 }}>
          <button
            className="primary-cta"
            type="button"
            onClick={() => onConfirm(employee)}
            style={{ flex: 1, justifyContent: 'center' }}
          >
            <span>Sí</span>
          </button>
          <button
            className="primary-cta"
            type="button"
            onClick={onCancel}
            style={{ flex: 1, justifyContent: 'center' }}
          >
            <span>No</span>
          </button>
        </div>
      </div>
    </div>
  );
};

ConfirmDelete.propTypes = {
  employee: PropTypes.object,
  onCancel: PropTypes.func,
  onConfirm: PropTypes.func,
  open: PropTypes.bool,
};

export default ConfirmDelete;
