import React from 'react';
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
      window.location.href = cat.href;
    }
  };

  return (
    <a className="cat-card" href="#" onClick={handleClick}>
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
    </a>
  );
};


const CategoriesGrid = ({ categories = [] }) => {
  return (
    <section className="categories-section max-w-6xl mx-auto my-8">
      <h2 className="section-title">Categorías populares</h2>
      <div className="categories-grid">
        {categories.map((c) => (
          <CategoryCard key={c.id} cat={c} />
        ))}
      </div>
    </section>
  );
};

export default CategoriesGrid;
