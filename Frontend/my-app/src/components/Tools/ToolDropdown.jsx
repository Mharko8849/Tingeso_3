import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/http-common";
import "./ToolDropdown.css";

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

  // Fetch categories from backend when component mounts
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const response = await api.get('/categories/');
        // Extract category names from the response
        const categoryNames = response.data.map(cat => cat.name);
        setCategories(categoryNames);
      } catch (error) {
        console.error('Error loading categories:', error);
        // Keep empty array on error
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  const toggleMenu = () => setOpen(!open);
  const closeMenu = () => setOpen(false);

  const handleCategoryClick = (category) => {
    navigate("/inventory", { state: { initialFilters: { category } } });
    closeMenu();
  };

  return (
    <>
      <button className="tool-btn" onClick={toggleMenu}>
        Herramientas
      </button>

      {open && (
        <div className="tool-overlay" onClick={closeMenu}>
          <div
            className="tool-content"
            onClick={(e) => e.stopPropagation()} // Prevent closing when clicking inside content
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
                  <a href="#" onClick={(e) => { e.preventDefault(); handleCategoryClick(''); }} className="category-item all-categories">
                    Todas
                  </a>
                  {categories.map((cat) => (
                    <a 
                      key={cat} 
                      href="#" 
                      onClick={(e) => { e.preventDefault(); handleCategoryClick(cat); }}
                      className="category-item"
                    >
                      {cat}
                    </a>
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
