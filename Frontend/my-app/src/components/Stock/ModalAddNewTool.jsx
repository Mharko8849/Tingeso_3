import React, { useState, useEffect, useCallback } from 'react';
import api from '../../services/http-common';
import Cropper from 'react-easy-crop';
import './ModalAddStockTool.css';

const ModalAddNewTool = ({ open, onClose, onAdded }) => {
  const [form, setForm] = useState({ toolName: '', category: '', repoCost: '', priceRent: '', priceFineAtDate: '' });

  // File state
  const [selectedImage, setSelectedImage] = useState(null); // The raw file url
  const [croppedImageBlob, setCroppedImageBlob] = useState(null); // The final blob to upload
  const [isDragging, setIsDragging] = useState(false);

  // Cropper state
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);
  const [showCropper, setShowCropper] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [categoriesList, setCategoriesList] = useState([]);

  useEffect(() => {
    if (open) {
      const fetchCategories = async () => {
        try {
          const response = await api.get('/categories/');
          setCategoriesList(response.data.map(c => c.name));
        } catch (error) {
          console.error('Error fetching categories:', error);
        }
      };
      fetchCategories();
    }
  }, [open]);

  // Clean up object URLs to avoid memory leaks
  useEffect(() => {
    return () => {
      if (selectedImage) URL.revokeObjectURL(selectedImage);
    };
  }, [selectedImage]);

  /* Helper to create the cropped image from the crop area */
  const createCroppedImage = async (imageSrc, pixelCrop) => {
    const image = new Image();
    image.src = imageSrc;
    await new Promise((resolve) => { image.onload = resolve; });

    const canvas = document.createElement('canvas');
    canvas.width = pixelCrop.width;
    canvas.height = pixelCrop.height;
    const ctx = canvas.getContext('2d');

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
      }, 'image/jpeg');
    });
  };

  const onCropComplete = useCallback((croppedArea, croppedAreaPixels) => {
    setCroppedAreaPixels(croppedAreaPixels);
  }, []);

  if (!open) return null;

  const handleConfirm = async () => {
    setLoading(true);
    setError(null);
    try {
      // Validate form
      if (!form.toolName || String(form.toolName).trim().length === 0) throw new Error('Nombre de herramienta requerido');
      if (!form.category) throw new Error('Categoría requerida');

      // If we are currently cropping (image selected but not confirmed), try to finish crop
      let finalBlob = croppedImageBlob;
      if (showCropper && selectedImage && croppedAreaPixels) {
        finalBlob = await createCroppedImage(selectedImage, croppedAreaPixels);
      }

      const repoCost = Number(form.repoCost) || 0;
      const priceRent = Number(form.priceRent) || 0;
      const priceFineAtDate = Number(form.priceFineAtDate) || 0;

      const me = await api.get('/api/user/me');
      const userId = me.data?.id;
      if (!userId) throw new Error('Usuario no identificado');

      const tool = {
        toolName: String(form.toolName).trim(),
        category: form.category ? { name: form.category } : null,
        repoCost: Math.round(repoCost),
        priceRent: Math.round(priceRent),
        priceFineAtDate: Math.round(priceFineAtDate),
      };

      const formData = new FormData();
      formData.append('tool', new Blob([JSON.stringify(tool)], { type: 'application/json' }));

      if (finalBlob) {
        // Send the cropped blob
        formData.append('image', finalBlob, 'tool-image.jpg');
      }

      await api.post(`/api/tool/user/${userId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (onAdded) onAdded();
      handleClose();
    } catch (e) {
      console.warn('Failed to add new tool', e);
      const msg = e?.response?.data || e?.message || 'No se pudo crear la herramienta';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (file) => {
    if (!file) return;
    if (selectedImage) URL.revokeObjectURL(selectedImage);
    const url = URL.createObjectURL(file);
    setSelectedImage(url);
    setShowCropper(true);
    setCroppedImageBlob(null); // Reset until crop confirmed
  };

  const handleDragOver = (e) => { e.preventDefault(); e.stopPropagation(); setIsDragging(true); };
  const handleDragLeave = (e) => { e.preventDefault(); e.stopPropagation(); setIsDragging(false); };
  const handleDrop = (e) => {
    e.preventDefault(); e.stopPropagation(); setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) handleFileSelect(file);
  };

  const confirmCrop = async () => {
    try {
      const blob = await createCroppedImage(selectedImage, croppedAreaPixels);
      setCroppedImageBlob(blob);
      setShowCropper(false); // Hide cropper, show preview of result
    } catch (e) {
      console.error(e);
    }
  };

  const handleClose = () => {
    setForm({ toolName: '', category: '', repoCost: '', priceRent: '', priceFineAtDate: '' });
    setSelectedImage(null);
    setCroppedImageBlob(null);
    setShowCropper(false);
    setZoom(1);
    onClose();
  };

  return (
    <div className="mas-backdrop">
      <div className="mas-modal mas-modal-large" style={{ position: 'relative' }}>
        <button className="mas-close" onClick={handleClose} aria-label="Cerrar">×</button>
        <h3 className="mas-title">Añadir nueva herramienta</h3>
        <div className="mas-content">

          {/* Form Fields */}
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
              {categoriesList.map((cat) => <option key={cat} value={cat}>{cat}</option>)}
            </select>
          </div>
          <div className="mas-groups-row">
            <div className="mas-row" style={{ flex: 1 }}>
              <label>Precio Reposición</label>
              <input type="number" value={form.repoCost} onChange={(e) => setForm((s) => ({ ...s, repoCost: e.target.value }))} />
            </div>
            <div className="mas-row" style={{ flex: 1 }}>
              <label>Precio Arriendo</label>
              <input type="number" value={form.priceRent} onChange={(e) => setForm((s) => ({ ...s, priceRent: e.target.value }))} />
            </div>
            <div className="mas-row" style={{ flex: 1 }}>
              <label>Multa diarias</label>
              <input type="number" value={form.priceFineAtDate} onChange={(e) => setForm((s) => ({ ...s, priceFineAtDate: e.target.value }))} />
            </div>
          </div>

          <div className="mas-row">
            <label>Imagen</label>

            {showCropper ? (
              <div className="cropper-container">
                <div className="cropper-wrap">
                  <Cropper
                    image={selectedImage}
                    crop={crop}
                    zoom={zoom}
                    aspect={1}
                    onCropChange={setCrop}
                    onCropComplete={onCropComplete}
                    onZoomChange={setZoom}
                  />
                </div>
                <div className="slider-container">
                  <label>Zoom</label>
                  <input
                    type="range"
                    value={zoom}
                    min={1}
                    max={3}
                    step={0.1}
                    aria-labelledby="Zoom"
                    onChange={(e) => setZoom(e.target.value)}
                    className="zoom-range"
                  />
                </div>
                <button className="mas-btn mas-confirm small-btn" onClick={confirmCrop} type="button">
                  Confirmar Recorte
                </button>
              </div>
            ) : (
              <div
                className={`mas-file-wrapper ${isDragging ? 'mas-file-wrapper-dragging' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                <label className="mas-file-button">
                  {croppedImageBlob || selectedImage ? 'Cambiar imagen' : 'Seleccionar imagen'}
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => handleFileSelect(e.target.files?.[0])}
                    style={{ display: 'none' }}
                  />
                </label>

                {croppedImageBlob && (
                  <div style={{ marginTop: 15, textAlign: 'center' }}>
                    <img
                      src={URL.createObjectURL(croppedImageBlob)}
                      alt="Preview"
                      style={{ width: 120, height: 120, borderRadius: 8, objectFit: 'cover', border: '1px solid #ddd' }}
                    />
                    <div className="mas-file-name">Imagen lista para subir</div>
                  </div>
                )}

                {!croppedImageBlob && !selectedImage && (
                  <div className="mas-file-name">
                    Arrastra una imagen o haz clic para subir
                  </div>
                )}
              </div>
            )}
          </div>

          {error && <div className="mas-error">{error}</div>}
        </div>

        <div className="mas-actions">
          <button className="mas-btn mas-confirm" onClick={handleConfirm} disabled={loading || showCropper}>
            {loading ? 'Guardando...' : 'Añadir Herramienta'}
          </button>
          <button className="mas-btn mas-cancel" onClick={handleClose} disabled={loading}>Cancelar</button>
        </div>
      </div>
    </div>
  );
};

export default ModalAddNewTool;
