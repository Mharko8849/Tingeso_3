import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/AlertContext';
import { classifyError } from '../../utils/classifyError';
import './ModalAddStockTool.css';

const ModalAddStockTool = ({ open, onClose, toolId, onAdded }) => {
  // allow empty string while typing, validate on blur/confirm
  const [quantity, setQuantity] = useState('');
  const [loading, setLoading] = useState(false);
  const [toolName, setToolName] = useState('');
  const [loadingName, setLoadingName] = useState(false);
  const { show } = useAlert();

  const handleNumericInput = (val) => {
    const cleaned = val.replaceAll(/\D/g, '');
    setQuantity(cleaned);
  };

  const handleConfirm = async () => {
    // Validate quantity
    const qty = Number(quantity || 0);
    if (!qty || qty < 1 || !Number.isInteger(qty)) {
      show({ severity: 'warning', message: 'La cantidad debe ser un número entero mayor a 0.', autoHideMs: 3500 });
      return;
    }

    setLoading(true);
    try {
      // resolve current user id
      const me = await api.get('/api/user/me');
      const userId = me.data?.id;
      if (!userId) throw new Error('Usuario no identificado. Inicie sesión nuevamente.');

      // call backend add-stock endpoint
      await api.post(`/api/inventory/add-stock/${userId}/${toolId}`, null, { params: { quantity: qty } });

      show({ severity: 'success', message: `Se ha añadido ${qty} unidad${qty > 1 ? 'es' : ''} al stock disponible correctamente.`, autoHideMs: 3500 });

      setTimeout(() => {
        if (onAdded) onAdded();
        onClose();
      }, 800);
    } catch (e) {
      console.warn('Failed to add stock', e);
      show({ severity: 'error', message: classifyError(e), autoHideMs: 6000 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!open || !toolId) return;
    let mounted = true;
    setLoadingName(true);
    setQuantity('');
    (async () => {
      try {
        const res = await api.get('/api/inventory/filter', { params: { idTool: toolId } });
        const arr = res.data || [];
        const t = arr[0]?.idTool || {};
        if (!mounted) return;
        setToolName(t.toolName || t.name || '');
      } catch (e) {
        console.warn('Failed to fetch tool name', e);
        if (mounted) setToolName('');
      } finally {
        if (mounted) setLoadingName(false);
      }
    })();
    return () => { mounted = false; };
  }, [open, toolId]);

  if (!open) return null;

  return (
    <div className="mas-backdrop">
    <button type="button" onClick={onClose} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
      <div className="mas-modal" style={{ position: 'relative' }}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Añadir stock</h3>

        <p style={{ marginTop: 8 }}>Herramienta ID: <strong>{toolId}</strong></p>
        {(() => { if (loadingName) { return <p style={{ marginTop: 4 }}>Cargando nombre...</p>; } if (toolName) { return <p style={{ marginTop: 4 }}>Nombre: <strong>{toolName}</strong></p>; } return null; })()}
        <div className="mas-row">
          <label htmlFor="mas-stock-quantity">Cantidad</label>
          <input
            id="mas-stock-quantity"
            type="number"
            min="1"
            step="1"
            placeholder="1"
            value={quantity}
            onChange={(e) => handleNumericInput(e.target.value)}
            onBlur={() => {
              const num = Number(quantity || 0);
              if (quantity !== '' && (!num || num < 1)) setQuantity('1');
            }}
          />
        </div>
        <div className="mas-actions">
          <button className="mas-btn mas-confirm" onClick={handleConfirm} disabled={loading}>{loading ? 'Guardando...' : 'Confirmar'}</button>
          <button className="mas-btn mas-cancel" onClick={onClose} disabled={loading}>Cancelar</button>
        </div>
      </div>
    </div>
  );
};

ModalAddStockTool.propTypes = {
  onAdded: PropTypes.func,
  onClose: PropTypes.func,
  open: PropTypes.bool,
  toolId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default ModalAddStockTool;
