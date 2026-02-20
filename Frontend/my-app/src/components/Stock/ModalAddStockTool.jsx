import React, { useEffect, useState } from 'react';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/AlertContext';
import './ModalAddStockTool.css';

const ModalAddStockTool = ({ open, onClose, toolId, onAdded }) => {
  // allow empty string while typing, validate on blur/confirm
  const [quantity, setQuantity] = useState('');
  const [loading, setLoading] = useState(false);
  const [toolName, setToolName] = useState('');
  const [loadingName, setLoadingName] = useState(false);
  const { show } = useAlert();

  // Classify errors for user-friendly messages
  const classifyError = (e) => {
    if (!e) return 'Ocurrió un error desconocido. Contacte al administrador.';

    // Network / connection errors
    if (e.code === 'ERR_NETWORK' || e.message === 'Network Error' || !e.response) {
      return 'Error de conexión. Verifique su conexión a internet e intente nuevamente.';
    }

    // HTTP status-based errors
    const status = e.response?.status;
    if (status === 401 || status === 403) {
      return 'No tiene permisos para realizar esta acción. Inicie sesión nuevamente.';
    }
    if (status === 400) {
      const data = e.response?.data;
      return typeof data === 'string' ? data : 'Datos inválidos. Revise la cantidad e intente nuevamente.';
    }
    if (status >= 500) {
      return 'Error interno del servidor. Contacte al administrador del sistema.';
    }

    // Validation error from throw new Error(...)
    if (e.message) return e.message;

    return 'No se pudo añadir stock. Contacte al administrador.';
  };

  const handleQuantityChange = (rawValue) => {
    // Allow empty string for clearing
    if (rawValue === '') {
      setQuantity('');
      return;
    }
    // Only allow positive integers (digits only, no negatives, no decimals)
    if (/^\d+$/.test(rawValue)) {
      setQuantity(rawValue);
    } else {
      show({ severity: 'warning', message: 'Ingrese un valor válido: número entero mayor a 0.', autoHideMs: 3500 });
    }
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
        const t = (arr[0] && arr[0].idTool) || {};
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
    <div className="mas-backdrop" onClick={onClose}>
      <div className="mas-modal" style={{ position: 'relative' }} onClick={(e) => e.stopPropagation()}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Añadir stock</h3>

        <p style={{ marginTop: 8 }}>Herramienta ID: <strong>{toolId}</strong></p>
        {loadingName ? <p style={{ marginTop: 4 }}>Cargando nombre...</p> : (toolName ? <p style={{ marginTop: 4 }}>Nombre: <strong>{toolName}</strong></p> : null)}
        <div className="mas-row">
          <label>Cantidad</label>
          <input
            type="text"
            inputMode="numeric"
            placeholder="1"
            value={quantity}
            onChange={(e) => handleQuantityChange(e.target.value)}
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

export default ModalAddStockTool;
