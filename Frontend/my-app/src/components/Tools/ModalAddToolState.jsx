import React, { useState } from 'react';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/useAlert';
import Badge from '../Badges/Badge';
import '../Stock/ModalAddStockTool.css';

const ModalAddToolState = ({ open, onClose, onAdded }) => {
  const [name, setName] = useState('');
  const [color, setColor] = useState('#6b7280');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { show } = useAlert();

  if (!open) return null;

  const handleConfirm = async () => {
    setLoading(true);
    setError(null);
    try {
      if (!name || name.trim().length === 0) {
        throw new Error('El nombre del estado es requerido');
      }

      await api.post(`/api/tool-states/`, { state: name.trim(), color: color });

      show({ message: 'Estado creado exitosamente', severity: 'success' });
      
      if (onAdded) onAdded();
      setName('');
      setColor('#6b7280');
      onClose();
    } catch (e) {
      console.warn('Failed to add tool state', e);
      const msg = e?.response?.data?.message || e?.message || 'No se pudo crear el estado';
      setError(msg);
      show({ message: msg, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mas-backdrop" onClick={onClose} style={{ zIndex: 10001 }}>
      <div className="mas-modal" style={{ width: '550px', position: 'relative' }} onClick={(e) => e.stopPropagation()}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Crear nuevo Estado</h3>
        <div className="mas-content">
          {/* Fila única horizontal con color y nombre */}
          <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', marginBottom: '12px' }}>
            {/* Sección de color */}
            <div style={{ flex: '0 0 auto' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '14px', fontWeight: 500 }}>
                Color del estado
              </label>
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                <input 
                  type="color"
                  value={color} 
                  onChange={(e) => setColor(e.target.value)} 
                  style={{ 
                    width: '50px', 
                    height: '50px', 
                    border: '2px solid #e2e8f0', 
                    borderRadius: '8px', 
                    cursor: 'pointer',
                    padding: '4px'
                  }}
                />
                {/* Badge preview usando el componente Badge real */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                  <Badge 
                    useClasses={false}
                    style={{ backgroundColor: color, boxShadow: `0 0 6px ${color}80` }} 
                  />
                  <span style={{ fontSize: '11px', color: '#64748b' }}>{color}</span>
                </div>
              </div>
            </div>

            {/* Sección de nombre */}
            <div style={{ flex: '1', minWidth: 0 }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '14px', fontWeight: 500 }}>
                Nombre del estado
              </label>
              <input 
                value={name} 
                onChange={(e) => setName(e.target.value.toUpperCase())} 
                placeholder="Ej. EN_REPARACION"
                autoFocus
                style={{ 
                  width: '100%',
                  padding: '10px 12px',
                  border: '1px solid #ccc',
                  borderRadius: '6px',
                  fontSize: '14px',
                  backgroundColor: '#ffffff',
                  color: '#000000'
                }}
              />
              <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px', fontStyle: 'italic' }}>
                * El texto se guardará en MAYÚSCULAS
              </div>
            </div>
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

export default ModalAddToolState;
