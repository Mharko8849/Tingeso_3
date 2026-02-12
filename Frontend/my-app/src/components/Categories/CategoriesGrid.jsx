import React from 'react';
import { Link } from 'react-router-dom';
import './CategoriesGrid.css';

const CategoryCard = ({ cat }) => {
  const isInternal = cat.href && cat.href.startsWith('/');
  
  if (isInternal) {
    return (
      <Link className="cat-card" to={cat.href}>
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
      </Link>
    );
  }

  return (
    <a className="cat-card" href={cat.href}>
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
