import React from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import './CategoriesGrid.css';

const CategoryCard = ({ cat }) => {
  const navigate = useNavigate();
  
  const handleClick = (e) => {
    e.preventDefault();
    if (cat.categoryName) {
      // Navigate to /inventory with category filter in state
      navigate('/inventory', { state: { initialFilters: { category: cat.categoryName } } });
    } else if (cat.href) {
      // Fallback for external links
      globalThis.location.href = cat.href;
    }
  };

  return (
    <button type="button" className="cat-card" onClick={handleClick} style={{ all: 'unset', display: 'flex', width: '100%', cursor: 'pointer' }}>
      <div className="cat-left">
        <h4 className="cat-title">{cat.title}</h4>
        <p className="cat-sub">{cat.subtitle}</p>
        <div className="cat-cta">
          <button className="cat-btn">Ver ofertas</button>
        </div>
      </div>
      <div className="cat-right">
        <img src={cat.image} alt={cat.title} style={{ borderLeft: `6px solid ${cat.color || '#2B7FFF'}` }} />
      </div>
    </button>
  );
};

CategoryCard.propTypes = {
  cat: PropTypes.shape({
    categoryName: PropTypes.string,
    href: PropTypes.string,
    title: PropTypes.string,
    subtitle: PropTypes.string,
    image: PropTypes.string,
    color: PropTypes.string,
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
};

const CategoriesGrid = ({ categories = [] }) => (
    <section className="categories-section max-w-6xl mx-auto my-8">
      <h2 className="section-title">Categorías populares</h2>
      <div className="categories-grid">
        {categories.map((c) => (
          <CategoryCard key={c.id} cat={c} />
        ))}
      </div>
    </section>
  );

CategoriesGrid.propTypes = {
  categories: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      categoryName: PropTypes.string,
      title: PropTypes.string,
      subtitle: PropTypes.string,
      image: PropTypes.string,
      color: PropTypes.string,
    }),
  ),
};

export default CategoriesGrid;
