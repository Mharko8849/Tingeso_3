import React, { useState } from 'react';
import PropTypes from 'prop-types';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/useAlert';
import '../Stock/ModalAddStockTool.css'; // Assuming this CSS exists in Tingeso_3, need to check

const ModalAddCategory = ({ open, onClose, onAdded }) => {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { show } = useAlert();

  if (!open) return null;

  const handleConfirm = async () => {
    setLoading(true);
    setError(null);
    try {
      if (!name || name.trim().length === 0) {
        throw new Error('El nombre de la categoría es requerido');
      }

      await api.post('/api/categories/', { name: name.trim() });

      show({ message: 'Categoría creada exitosamente', severity: 'success' });
      
      if (onAdded) onAdded();
      setName('');
      onClose();
    } catch (e) {
      console.warn('Failed to add category', e);
      const msg = e?.response?.data?.message || e?.message || 'No se pudo crear la categoría';
      setError(msg);
      show({ message: msg, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mas-backdrop" style={{ zIndex: 10001 }}>
    <button type="button" onClick={onClose} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
      <div className="mas-modal" style={{ width: '400px', position: 'relative' }}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Crear nueva Categoría</h3>
        <div className="mas-content">
          <div className="mas-row">
            <label htmlFor="category-name-input">Nombre de la categoría</label>
            <input id="category-name-input" 
              value={name} 
              onChange={(e) => setName(e.target.value)} 
              placeholder="Ej. Herramientas Manuales"
              autoFocus
            />
          </div>

          {error && <div className="mas-error">{error}</div>}
        </div>

        <div className="mas-actions">
          <button className="mas-btn mas-confirm" onClick={handleConfirm} disabled={loading}>
            {loading ? 'Creando...' : 'Crear'}
          </button>
          <button className="mas-btn mas-cancel" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
};

ModalAddCategory.propTypes = {
  onAdded: PropTypes.func,
  onClose: PropTypes.func,
  open: PropTypes.bool,
};

export default ModalAddCategory;
