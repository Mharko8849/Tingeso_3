import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import api from '../../services/http-common';
import { useAlert } from '../Alerts/AlertContext';
import './ModalAddStockTool.css';
import useDragAndDrop from '../../hooks/useDragAndDrop';
import { handleNumericInput } from '../../utils/numericInput';
import { classifyError } from '../../utils/classifyError';

const ModalEditTool = ({ open, onClose, tool, onUpdated }) => {
  const [form, setForm] = useState({ toolName: '', category: '', repoCost: '', priceRent: '', priceFineAtDate: '' });
  const [file, setFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [categoriesList, setCategoriesList] = useState([]);
  const { show } = useAlert();
  const alert = useAlert();

  const onFileDrop = useCallback((droppedFile, dataURL) => {
    setImagePreview(dataURL);
    setFile(droppedFile);
  }, []);
  const { dropRef, isDragging } = useDragAndDrop({ onFileDrop });

  useEffect(() => {
    if (open) {
      const fetchCategories = async () => {
        try {
          const response = await api.get('/api/categories/');
          setCategoriesList(response.data.map(c => c.name));
        } catch (error) {
          console.error('Error fetching categories:', error);
        }
      };
      fetchCategories();
    }
  }, [open]);

  useEffect(() => {
    if (tool) {
      setForm({
        toolName: tool.toolName || tool.name || '',
        category: (typeof tool.category === 'string' ? tool.category : tool.category?.name) || '',
        repoCost: tool.repoCost != null ? String(tool.repoCost) : '',
        priceRent: (() => {
          if (tool.priceRent != null) return String(tool.priceRent);
          if (tool.price != null) return String(tool.price);
          return '';
        })(),
        priceFineAtDate: tool.priceFineAtDate != null ? String(tool.priceFineAtDate) : '',
      });
      setFile(null);
      setImagePreview(null);
    }
  }, [tool]);

  if (!open || !tool) return null;

  // Validate numeric fields: must be integers >= 0
  const validateNumericFields = () => {
    const fields = [
      { value: form.repoCost, label: 'Precio reposición' },
      { value: form.priceRent, label: 'Precio arriendo' },
      { value: form.priceFineAtDate, label: 'Tarifa multa por día' },
    ];
    for (const field of fields) {
      if (field.value === '' || field.value === undefined || field.value === null) continue;
      const num = Number(field.value);
      if (Number.isNaN(num) || !Number.isInteger(num) || num < 0) {
        return `${field.label}: Ingrese un valor válido, entero mayor o igual a 0.`;
      }
    }
    return null;
  };

  const handleConfirm = async () => {
    if (!form.toolName || String(form.toolName).trim().length === 0) {
      show({ severity: 'warning', message: 'Nombre de herramienta requerido.', autoHideMs: 3500 });
      return;
    }
    const numError = validateNumericFields();
    if (numError) {
      show({ severity: 'warning', message: numError, autoHideMs: 3500 });
      return;
    }

    setLoading(true);
    try {
      const repoCost = Number(form.repoCost) || 0;
      const priceRent = Number(form.priceRent) || 0;
      const priceFineAtDate = Number(form.priceFineAtDate) || 0;

      const me = await api.get('/api/user/me');
      const userId = me.data?.id;
      if (!userId) throw new Error('Usuario no identificado. Inicie sesión nuevamente.');

      const toolPayload = {
        toolName: String(form.toolName).trim(),
        category: form.category ? { name: form.category } : null,
        repoCost: Math.round(repoCost),
        priceRent: Math.round(priceRent),
        priceFineAtDate: Math.round(priceFineAtDate),
      };

      const formData = new FormData();
      formData.append('tool', new Blob([JSON.stringify(toolPayload)], { type: 'application/json' }));
      if (file) formData.append('image', file);

      const toolId = tool.id || tool.idTool;
      const response = await api.put(`/api/tool/${toolId}/user/${userId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (onUpdated) onUpdated(response.data);
      onClose();
    } catch (e) {
      console.warn('Failed to edit tool', e);
      show({ severity: 'error', message: classifyError(e), autoHideMs: 6000 });
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (fileObj) => {
    if (!fileObj) {
      setFile(null);
      setImagePreview(null);
      return;
    }
    setFile(fileObj);
    const reader = new FileReader();
    reader.onloadend = () => {
      setImagePreview(reader.result);
    };
    reader.readAsDataURL(fileObj);
  };

  return (
    <div className="mas-backdrop">
    <button type="button" onClick={onClose} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
      <div className="mas-modal mas-modal-large" style={{ position: 'relative' }}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Editar herramienta</h3>
        <div className="mas-content">
          <div className="mas-row">
            <label htmlFor="met-name">Nombre</label>
            <input id="met-name" value={form.toolName} onChange={(e) => setForm((s) => ({ ...s, toolName: e.target.value }))} />
          </div>

          <div className="mas-row">
            <label htmlFor="met-category">Categoría</label>
            <select
              id="met-category"
              value={form.category}
              onChange={(e) => setForm((s) => ({ ...s, category: e.target.value }))}
              style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc' }}
            >
              <option value="">Seleccionar categoría</option>
              {categoriesList.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          <div className="mas-row">
            <label htmlFor="met-repo-cost">Precio reposición</label>
            <input
              id="met-repo-cost"
              type="number"
              min="0"
              step="1"
              placeholder="0"
              value={form.repoCost}
              onChange={(e) => handleNumericInput('repoCost', e.target.value, setForm, alert)}
            />
          </div>

          <div className="mas-row">
            <label htmlFor="met-price-rent">Precio arriendo</label>
            <input
              id="met-price-rent"
              type="number"
              min="0"
              step="1"
              placeholder="0"
              value={form.priceRent}
              onChange={(e) => handleNumericInput('priceRent', e.target.value, setForm, alert)}
            />
          </div>

          <div className="mas-row">
            <label htmlFor="met-price-fine">Tarifa multa por día</label>
            <input
              id="met-price-fine"
              type="number"
              min="0"
              step="1"
              placeholder="0"
              value={form.priceFineAtDate}
              onChange={(e) => handleNumericInput('priceFineAtDate', e.target.value, setForm, alert)}
            />
          </div>

          <div className="mas-row">
            <label htmlFor="met-image">Imagen (opcional)</label>
            <div
              ref={dropRef}
              className={`mas-file-wrapper ${isDragging ? 'mas-file-wrapper-dragging' : ''}`}
            >
              <label className="mas-file-button">
                <span>Seleccionar archivo</span>
                <input
                  id="met-image"
                  type="file"
                  accept="image/*"
                  onChange={(e) => handleFileSelect(e.target.files?.[0] || null)}
                  style={{ display: 'none' }}
                />
              </label>
              <div className="mas-file-name">
                {file
                  ? file.name
                  : 'Haz clic para seleccionar o arrastra y suelta una nueva imagen aquí'}
              </div>
            </div>

            {imagePreview && (
              <div style={{ marginTop: '12px', textAlign: 'center' }}>
                <img
                  src={imagePreview}
                  alt="Preview"
                  style={{ maxWidth: '200px', maxHeight: '200px', borderRadius: '8px', border: '2px solid #007bff' }}
                />
              </div>
            )}
          </div>
        </div>

        <div className="mas-actions">
          <button className="mas-btn mas-confirm" onClick={handleConfirm} disabled={loading}>
            {loading ? 'Guardando...' : 'Guardar cambios'}
          </button>
          <button className="mas-btn mas-cancel" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
};

ModalEditTool.propTypes = {
  onClose: PropTypes.func,
  onUpdated: PropTypes.func,
  open: PropTypes.bool,
  tool: PropTypes.object,
};

export default ModalEditTool;
