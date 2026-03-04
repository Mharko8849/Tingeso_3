import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/http-common';
import './ToolDropdown.css';

/**
 * ToolDropdown component.
 * Displays a dropdown menu with tool categories for navigation.
 * Categories are loaded dynamically from the backend.
 */
const ToolDropdown = () => {
  const [open, setOpen] = useState(false);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // Fetch categories from backend
  const fetchCategories = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/categories/');
      // Extract category names from the response
      const categoryNames = response.data.map(cat => cat.name);
      setCategories(categoryNames);
    } catch (error) {
      console.error('Error loading categories:', error);
      // Keep empty array on error
      setCategories([]);
    } finally {
      setLoading(false);
    }
  };

  // Fetch categories when component mounts
  useEffect(() => {
    fetchCategories();
  }, []);

  // Refetch categories when dropdown opens
  useEffect(() => {
    if (open) {
      fetchCategories();
    }
  }, [open]);

  const toggleMenu = () => setOpen(!open);
  const closeMenu = () => setOpen(false);

  const handleCategoryClick = (category) => {
    navigate('/inventory', { state: { initialFilters: { category } } });
    closeMenu();
  };

  return (
    <>
      <button className="tool-btn" onClick={toggleMenu}>
        Herramientas
      </button>

      {open && (
        <div className="tool-overlay" >
      <button type="button" onClick={closeMenu} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
          <div
            className="tool-content"
            // Prevent closing when clicking inside content
          >
            <button className="close-btn" onClick={closeMenu}>
              ✕
            </button>

            <div className="tool-grid-container">
              <h4>Categorías</h4>
              {loading ? (
                <div className="tool-categories-grid">
                  <p style={{ gridColumn: '1 / -1', textAlign: 'center', color: '#666' }}>
                    Cargando categorías...
                  </p>
                </div>
              ) : (
                <div className="tool-categories-grid">
                  <button type="button" onClick={() => handleCategoryClick('')} className="category-item all-categories">
                    Todas
                  </button>
                  {categories.map((cat) => (
                    <button 
                      type="button"
                      key={cat} 
                      onClick={() => handleCategoryClick(cat)}
                      className="category-item"
                    >
                      {cat}
                    </button>
                  ))}
                  {categories.length === 0 && !loading && (
                    <p style={{ gridColumn: '1 / -1', textAlign: 'center', color: '#999' }}>
                      No hay categorías disponibles
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ToolDropdown;
