import React, { useState, useEffect } from 'react';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/useAlert';
import ModalAddToolState from './ModalAddToolState';
import Badge from '../Badges/Badge';
import '../Stock/ModalAddStockTool.css';
import './ModalManageToolStates.css';

const ModalManageToolStates = ({ open, onClose }) => {
  const [states, setStates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingState, setEditingState] = useState('');
  const [editingColor, setEditingColor] = useState('#6b7280');
  const [showAddModal, setShowAddModal] = useState(false);
  const { show } = useAlert();

  useEffect(() => {
    if (open) {
      fetchStates();
    }
  }, [open]);

  const fetchStates = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/tool-states/');
      // Una respuesta vacía es válida, no es un error
      setStates(response.data || []);
    } catch (error) {
      console.error('Error loading tool states:', error);
      // Solo mostrar alerta si hay un error REAL de conexión
      // No mostrar error si simplemente no hay datos o el servidor responde OK vacío
      if (error.request && !error.response) {
        // La petición se hizo pero no hubo respuesta - problema de conexión
        show({ message: 'No se pudo conectar con el servidor', severity: 'error' });
      } else if (error.response && error.response.status >= 500) {
        // Error del servidor (5xx) - problema real del backend
        show({ message: 'Error interno del servidor', severity: 'error' });
      }
      // Para cualquier otro caso (404, datos vacíos, etc), no mostramos alerta
      // El UI mostrará "No hay estados disponibles" automáticamente
      setStates([]);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (state) => {
    setEditingId(state.id);
    setEditingState(state.state);
    setEditingColor(state.color || '#6b7280');
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditingState('');
    setEditingColor('#6b7280');
  };

  const handleSaveEdit = async (id) => {
    if (!editingState || editingState.trim().length === 0) {
      show({ message: 'El nombre del estado no puede estar vacío', severity: 'error' });
      return;
    }

    try {
      await api.put(`/api/tool-states/${id}`, { state: editingState.trim(), color: editingColor });
      show({ message: 'Estado actualizado exitosamente', severity: 'success' });
      setEditingId(null);
      setEditingState('');
      setEditingColor('#6b7280');
      fetchStates();
    } catch (error) {
      console.error('Error updating state:', error);
      const msg = error?.response?.data?.message || 'Error al actualizar estado';
      show({ message: msg, severity: 'error' });
    }
  };

  const handleDelete = async (id, stateName) => {
    if (!window.confirm(`¿Estás seguro de eliminar el estado "${stateName}"?`)) {
      return;
    }

    try {
      await api.delete(`/api/tool-states/${id}`);
      show({ message: 'Estado eliminado exitosamente', severity: 'success' });
      fetchStates();
    } catch (error) {
      console.error('Error deleting state:', error);
      const msg = error?.response?.data?.message || 'Error al eliminar estado';
      show({ message: msg, severity: 'error' });
    }
  };

  const handleAddState = () => {
    setShowAddModal(true);
  };

  const handleStateAdded = () => {
    fetchStates();
  };

  if (!open) return null;

  return (
    <>
      <div className="mas-backdrop" onClick={onClose} style={{ zIndex: 10001 }}>
        <div className="mas-modal" style={{ width: '80%', maxWidth: '900px', position: 'relative' }} onClick={(e) => e.stopPropagation()}>
          <button className="mas-close" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
          <h3 className="mas-title">Estados de Herramientas</h3>
          
          <div className="mas-content" style={{ maxHeight: '60vh', overflowY: 'auto', marginBottom: '20px' }}>
            {loading ? (
              <div className="states-loading">Cargando estados...</div>
            ) : states.length === 0 ? (
              <div className="states-empty">No hay estados creados</div>
            ) : (
              <div className="states-list">
                {states.map((state) => (
                  <div key={state.id} className="state-item-row">
                    {editingId === state.id ? (
                      <>
                        <div style={{ display: 'flex', gap: '12px', flex: 1, alignItems: 'center' }}>
                          <input
                            type="color"
                            value={editingColor}
                            onChange={(e) => setEditingColor(e.target.value)}
                            style={{ width: '50px', height: '38px', border: '2px solid #2b6cb0', borderRadius: '6px', cursor: 'pointer' }}
                          />
                          <Badge
                            useClasses={false}
                            style={{ backgroundColor: editingColor, boxShadow: `0 0 6px ${editingColor}80` }}
                          />
                          <input
                            className="state-edit-input"
                            value={editingState}
                            onChange={(e) => setEditingState(e.target.value)}
                            autoFocus
                            onKeyPress={(e) => {
                              if (e.key === 'Enter') handleSaveEdit(state.id);
                              if (e.key === 'Escape') handleCancelEdit();
                            }}
                            style={{ flex: 1 }}
                          />
                        </div>
                        <div className="state-actions">
                          <button
                            className="state-btn state-btn-save"
                            onClick={() => handleSaveEdit(state.id)}
                            title="Guardar"
                          >
                            ✓
                          </button>
                          <button
                            className="state-btn state-btn-cancel"
                            onClick={handleCancelEdit}
                            title="Cancelar"
                          >
                            ✕
                          </button>
                        </div>
                      </>
                    ) : (
                      <>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                          <span 
                            style={{ 
                              display: 'inline-block',
                              width: '16px', 
                              height: '16px', 
                              borderRadius: '50%', 
                              backgroundColor: state.color || '#6b7280',
                              boxShadow: `0 0 6px ${state.color || '#6b7280'}80`,
                              flexShrink: 0
                            }}
                          />
                          <span className="state-name">{state.state}</span>
                        </div>
                        <div className="state-actions">
                          <button
                            className="state-btn state-btn-edit"
                            onClick={() => handleEdit(state)}
                            title="Editar"
                          >
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          </button>
                          <button
                            className="state-btn state-btn-delete"
                            onClick={() => handleDelete(state.id, state.state)}
                            title="Eliminar"
                          >
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="mas-actions" style={{ justifyContent: 'center' }}>
            <button className="mas-btn mas-add-new" onClick={handleAddState} style={{ width: '100%' }}>
              <span className="add-icon">+</span> Añadir nuevo estado
            </button>
          </div>
        </div>
      </div>

      <ModalAddToolState
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
        onAdded={handleStateAdded}
      />
    </>
  );
};

export default ModalManageToolStates;
