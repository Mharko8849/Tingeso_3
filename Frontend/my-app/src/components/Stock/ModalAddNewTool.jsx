import React, { useState, useEffect, useCallback } from 'react';
import api from '../../services/http-common';
import Cropper from 'react-easy-crop';
import './ModalAddStockTool.css';
import { useAlert } from '../Alerts/AlertContext';

const ModalAddNewTool = ({ open, onClose, onAdded }) => {
  const [form, setForm] = useState({ toolName: '', category: '', repoCost: '', priceRent: '', priceFineAtDate: '' });
  const [file, setFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [showCrop, setShowCrop] = useState(false);
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [categoriesList, setCategoriesList] = useState([]);

  const alert = useAlert();

  useEffect(() => {
    if (open) {
      const fetchCategories = async () => {
        try {
          const response = await api.get('/api/categories/');
          // response.data is array of objects {id, name}
          setCategoriesList(response.data.map(c => c.name)); 
        } catch (error) {
          console.error('Error fetching categories:', error);
        }
      };
      fetchCategories();
      // Reset states when opening
      setForm({ toolName: '', category: '', repoCost: '', priceRent: '', priceFineAtDate: '' });
      setFile(null);
      setImagePreview(null);
      setShowCrop(false);
      setError(null);
      setSuccess(false);
    }
  }, [open]);

  const onCropComplete = useCallback((croppedArea, croppedAreaPixels) => {
    setCroppedAreaPixels(croppedAreaPixels);
  }, []);

  if (!open) return null;

  const createImage = (url) =>
    new Promise((resolve, reject) => {
      const image = new Image();
      image.addEventListener('load', () => resolve(image));
      image.addEventListener('error', (error) => reject(error));
      image.src = url;
    });

  const getCroppedImg = async (imageSrc, pixelCrop) => {
    const image = await createImage(imageSrc);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    canvas.width = pixelCrop.width;
    canvas.height = pixelCrop.height;

    ctx.drawImage(
      image,
      pixelCrop.x,
      pixelCrop.y,
      pixelCrop.width,
      pixelCrop.height,
      0,
      0,
      pixelCrop.width,
      pixelCrop.height
    );

    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        resolve(blob);
      }, 'image/jpeg', 0.9);
    });
  };

  const handleSaveCrop = async () => {
    if (!imagePreview || !croppedAreaPixels) return;
    
    try {
      const croppedBlob = await getCroppedImg(imagePreview, croppedAreaPixels);
      const croppedFile = new File([croppedBlob], file.name || 'cropped-image.jpg', { type: 'image/jpeg' });
      setFile(croppedFile);
      setShowCrop(false);
      // Keep preview for display
    } catch (e) {
      console.error('Error al recortar imagen:', e);
      setError('No se pudo recortar la imagen');
    }
  };

  const handleConfirm = async () => {
    setLoading(true);
    setError(null);
    try {
      // validate
      if (!form.toolName || String(form.toolName).trim().length === 0) throw new Error('Nombre de herramienta requerido');
      if (!form.category) throw new Error('Categoría requerida');
      const repoCost = Number(form.repoCost) || 0;
      const priceRent = Number(form.priceRent) || 0;
      const priceFineAtDate = Number(form.priceFineAtDate) || 0;

      const me = await api.get('/api/user/me');
      const userId = me.data?.id;
      if (!userId) throw new Error('Usuario no identificado');

      // construir objeto tool que coincida con ToolEntity en backend
      const tool = {
        toolName: String(form.toolName).trim(),
        category: form.category ? { name: form.category } : null,
        repoCost: Math.round(repoCost),
        priceRent: Math.round(priceRent),
        priceFineAtDate: Math.round(priceFineAtDate),
      };

      // construir FormData multipart con parte "tool" (JSON) + "image"
      const formData = new FormData();
      formData.append('tool', new Blob([JSON.stringify(tool)], { type: 'application/json' }));

      if (file) {
        formData.append('image', file);
      }

      await api.post(`/api/tool/user/${userId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      setSuccess(true);
      setError(null);
      
      // Wait a bit to show success message, then close and refresh
      setTimeout(() => {
        if (onAdded) onAdded();
        onClose();
      }, 1500);
    } catch (e) {
      console.warn('Failed to add new tool', e);
      const msg = e?.response?.data || e?.message || 'No se pudo crear la herramienta';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (fileObj) => {
    if (!fileObj) {
      setFile(null);
      setImagePreview(null);
      setShowCrop(false);
      return;
    }
    setFile(fileObj);
    // Create preview URL
    const reader = new FileReader();
    reader.onloadend = () => {
      setImagePreview(reader.result);
      setShowCrop(true); // Show crop tool immediately
    };
    reader.readAsDataURL(fileObj);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files && e.dataTransfer.files[0];
    if (droppedFile && droppedFile.type.startsWith('image/')) {
      handleFileSelect(droppedFile);
    }
  };

  const handleNumericInput = (field, value) => {
    if (value === '' || /^\d+$/.test(value)) {
      setForm((s) => ({ ...s, [field]: value }));
    } else {
      alert.show({ severity: 'warning', message: 'Debe ingresar valores enteros positivos', autoHideMs: 3500 });
    }
  };

  return (
    <div className="mas-backdrop">
      <div className="mas-modal mas-modal-large" style={{ position: 'relative' }}>
        <button className="mas-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <h3 className="mas-title">Añadir nueva herramienta</h3>
        <div className="mas-content">
        <div className="mas-row">
          <label>Nombre</label>
          <input value={form.toolName} onChange={(e) => setForm((s) => ({ ...s, toolName: e.target.value }))} />
        </div>

        <div className="mas-row">
          <label>Categoría</label>
          <select 
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
          <label>Precio reposición</label>
          <input type="number" min="0" step="1" value={form.repoCost} onChange={(e) => handleNumericInput('repoCost', e.target.value)} />
        </div>

        <div className="mas-row">
          <label>Precio arriendo</label>
          <input type="number" min="0" step="1" value={form.priceRent} onChange={(e) => handleNumericInput('priceRent', e.target.value)} />
        </div>

        <div className="mas-row">
          <label>Tarifa multa por día</label>
          <input type="number" min="0" step="1" value={form.priceFineAtDate} onChange={(e) => handleNumericInput('priceFineAtDate', e.target.value)} />
        </div>

        <div className="mas-row">
          <label>Imagen (opcional)</label>
          <div
            className={`mas-file-wrapper ${isDragging ? 'mas-file-wrapper-dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <label className="mas-file-button">
              Seleccionar archivo
              <input
                type="file"
                accept="image/*"
                onChange={(e) => handleFileSelect(e.target.files?.[0] || null)}
                style={{ display: 'none' }}
              />
            </label>
            <div className="mas-file-name">
              {file
                ? file.name
                : 'Haz clic para seleccionar o arrastra y suelta una imagen aquí'}
            </div>
          </div>
          
          {/* Preview de imagen seleccionada (después del crop) */}
          {imagePreview && !showCrop && (
            <div style={{ marginTop: '12px', textAlign: 'center' }}>
              <img 
                src={imagePreview} 
                alt="Preview" 
                style={{ maxWidth: '200px', maxHeight: '200px', borderRadius: '8px', border: '2px solid #007bff' }} 
              />
              <button 
                type="button"
                onClick={() => setShowCrop(true)} 
                style={{ 
                  display: 'block', 
                  margin: '8px auto', 
                  padding: '6px 12px', 
                  background: '#007bff', 
                  color: 'white', 
                  border: 'none', 
                  borderRadius: '4px', 
                  cursor: 'pointer' 
                }}
              >
                Recortar imagen
              </button>
            </div>
          )}
        </div>

        {success && (
          <div style={{ 
            padding: '12px', 
            background: '#d4edda', 
            border: '1px solid #c3e6cb', 
            borderRadius: '4px', 
            color: '#155724',
            marginTop: '12px',
            textAlign: 'center',
            fontWeight: 'bold'
          }}>
            ✓ Herramienta creada exitosamente
          </div>
        )}

        {error && <div className="mas-error">{error}</div>}

        </div>

        <div className="mas-actions">
          <button className="mas-btn mas-confirm" onClick={handleConfirm} disabled={loading}>{loading ? 'Creando...' : 'Añadir'}</button>
          <button className="mas-btn mas-cancel" onClick={onClose} disabled={loading}>Cancelar</button>
        </div>
      </div>

      {/* Modal de recorte de imagen */}
      {showCrop && imagePreview && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.9)',
          zIndex: 10000,
          display: 'flex',
          flexDirection: 'column',
        }}>
          <div style={{
            position: 'relative',
            flex: 1,
            minHeight: 0,
          }}>
            <Cropper
              image={imagePreview}
              crop={crop}
              zoom={zoom}
              aspect={4 / 3}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={onCropComplete}
            />
          </div>
          
          <div style={{
            background: '#333',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <label style={{ color: 'white', minWidth: '60px' }}>Zoom:</label>
              <input
                type="range"
                min={1}
                max={3}
                step={0.1}
                value={zoom}
                onChange={(e) => setZoom(Number(e.target.value))}
                style={{ flex: 1 }}
              />
            </div>
            
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={handleSaveCrop}
                style={{
                  padding: '10px 24px',
                  background: '#007bff',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontWeight: 'bold',
                }}
              >
                Guardar recorte
              </button>
              <button
                onClick={() => setShowCrop(false)}
                style={{
                  padding: '10px 24px',
                  background: '#6c757d',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                }}
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ModalAddNewTool;
