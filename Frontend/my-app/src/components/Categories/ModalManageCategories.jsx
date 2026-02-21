import React, { useState, useEffect } from 'react';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/useAlert';
import { useEscapeKey } from '../../hooks/useKeyboardShortcuts';
import ModalAddCategory from './ModalAddCategory';
import '../Stock/ModalAddStockTool.css';
import './ModalManageCategories.css';

const ModalManageCategories = ({ open, onClose }) => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingName, setEditingName] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const { show } = useAlert();
  
  // Close modal with Escape key (Nielsen Heuristic #3: User Control and Freedom)
  useEscapeKey(onClose, open && !showAddModal);

  useEffect(() => {
    if (open) {
      fetchCategories();
    }
  }, [open]);

  const fetchCategories = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/categories/');
      // Una respuesta vacía es válida, no es un error
      setCategories(response.data || []);
    } catch (error) {
      console.error('Error loading categories:', error);
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
      // El UI mostrará "No hay categorías disponibles" automáticamente
      setCategories([]);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (category) => {
    setEditingId(category.id);
    setEditingName(category.name);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditingName('');
  };

  const handleSaveEdit = async (id) => {
    if (!editingName || editingName.trim().length === 0) {
      show({ message: 'El nombre de la categoría no puede estar vacío', severity: 'error' });
      return;
    }

    try {
      await api.put(`/api/categories/${id}`, { name: editingName.trim() });
      show({ message: 'Categoría actualizada exitosamente', severity: 'success' });
      setEditingId(null);
      setEditingName('');
      fetchCategories();
    } catch (error) {
      console.error('Error updating category:', error);
      const msg = error?.response?.data?.message || 'Error al actualizar categoría';
      show({ message: msg, severity: 'error' });
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`¿Estás seguro de eliminar la categoría "${name}"?\n\nEsta acción no se puede deshacer. Las herramientas asociadas a esta categoría quedarán sin categoría.`)) {
      return;
    }

    try {
      await api.delete(`/api/categories/${id}`);
      show({ message: 'Categoría eliminada exitosamente', severity: 'success' });
      fetchCategories();
    } catch (error) {
      console.error('Error deleting category:', error);
      const msg = error?.response?.data?.message || 'Error al eliminar categoría';
      show({ message: msg, severity: 'error' });
    }
  };

  const handleAddCategory = () => {
    setShowAddModal(true);
  };

  const handleCategoryAdded = () => {
    fetchCategories();
  };

  if (!open) return null;

  return (
    <>
      <div className="mas-backdrop" onClick={onClose} style={{ zIndex: 10001 }}>
        <div className="mas-modal" style={{ width: '80%', maxWidth: '900px', position: 'relative' }} onClick={(e) => e.stopPropagation()}>
          <button className="mas-close" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
          <h3 className="mas-title">Categorías de Herramientas</h3>
          
          <div className="mas-content" style={{ maxHeight: '60vh', overflowY: 'auto', marginBottom: '20px' }}>
            {loading ? (
              <div className="categories-loading">Cargando categorías...</div>
            ) : categories.length === 0 ? (
              <div className="categories-empty">No hay categorías creadas</div>
            ) : (
              <div className="categories-list">
                {categories.map((category) => (
                  <div key={category.id} className="category-item-row">
                    {editingId === category.id ? (
                      <>
                        <input
                          className="category-edit-input"
                          value={editingName}
                          onChange={(e) => setEditingName(e.target.value)}
                          autoFocus
                          onKeyPress={(e) => {
                            if (e.key === 'Enter') handleSaveEdit(category.id);
                            if (e.key === 'Escape') handleCancelEdit();
                          }}
                        />
                        <div className="category-actions">
                          <button
                            className="category-btn category-btn-save"
                            onClick={() => handleSaveEdit(category.id)}
                            title="Guardar"
                          >
                            ✓
                          </button>
                          <button
                            className="category-btn category-btn-cancel"
                            onClick={handleCancelEdit}
                            title="Cancelar"
                          >
                            ✕
                          </button>
                        </div>
                      </>
                    ) : (
                      <>
                        <span className="category-name">{category.name}</span>
                        <div className="category-actions">
                          <button
                            className="category-btn category-btn-edit"
                            onClick={() => handleEdit(category)}
                            title="Editar"
                          >
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          </button>
                          <button
                            className="category-btn category-btn-delete"
                            onClick={() => handleDelete(category.id, category.name)}
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
            <button className="mas-btn mas-add-new" onClick={handleAddCategory} style={{ width: '100%' }}>
              <span className="add-icon">+</span> Añadir nueva categoría
            </button>
          </div>
        </div>
      </div>

      <ModalAddCategory
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
        onAdded={handleCategoryAdded}
      />
    </>
  );
};

export default ModalManageCategories;
