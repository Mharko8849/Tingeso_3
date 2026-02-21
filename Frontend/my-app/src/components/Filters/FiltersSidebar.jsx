import React, { useState, useEffect } from 'react';
import api from '../../services/http-common';
import './FiltersSidebar.css';

/**
 * FiltersSidebar component.
 * Renders a sidebar with price range sliders and sorting options.
 */
const FiltersSidebar = ({ onApply = () => { }, initial = {} }) => {
  // Defines the minimum and maximum values and price steps.
  const BOUND_MIN = 0;
  const BOUND_MAX = 500000;
  const STEP = 5000;

  // Clamps a value between the minimum and maximum bounds.
  const clamp = (v) => Math.max(BOUND_MIN, Math.min(BOUND_MAX, v));

  // Initializes state with clamped initial values or defaults.
  const [minPrice, setMinPrice] = useState(clamp(initial.minPrice ?? BOUND_MIN));
  const [maxPrice, setMaxPrice] = useState(clamp(initial.maxPrice ?? BOUND_MAX));

  // Stores the current sort order. Empty string means 'no specific order'.
  const [sort, setSort] = useState(initial.sort ?? '');
  const [category, setCategory] = useState(initial.category ?? '');
  const [search, setSearch] = useState(initial.search ?? '');
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    const fetchCats = async () => {
      try {
        const res = await api.get('/api/categories/'); // Ensure trailing slash if controller has it
        setCategories(res.data.map(c => c.name));
      } catch (e) { console.error(e); }
    };
    fetchCats();
  }, []);

  // Resets all filters to their default values and notifies the parent component.
  const resetFilters = () => {
    setMinPrice(BOUND_MIN);
    setMaxPrice(BOUND_MAX);
    setSort('');
    setCategory('');
    setSearch('');

    // Mirrors the shape returned by apply() to ensure consistency.
    const filters = {
      minPrice: 0,
      maxPrice: BOUND_MAX,
      sort: '',
      category: '',
      search: '',
      asc: false,
      desc: false,
      recent: false,
    };
    onApply(filters);
  };

  // Synchronizes internal UI state when parent provides new initial filters.
  useEffect(() => {
    setMinPrice(clamp(initial.minPrice ?? BOUND_MIN));
    setMaxPrice(clamp(initial.maxPrice ?? BOUND_MAX));
    setSort(initial.sort ?? '');
    setCategory(initial.category ?? '');
    setSearch(initial.search ?? '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initial.minPrice, initial.maxPrice, initial.sort, initial.category, initial.search]);

  // Applies the selected filters and sorting options.
  const apply = () => {
    const asc = sort === 'price_asc';
    const desc = sort === 'price_desc';
    const recent = sort === 'newest';
    const popular = sort === 'popular';

    const filters = {
      minPrice: minPrice ? Number(minPrice) : 0,
      maxPrice: maxPrice ? Number(maxPrice) : 0,
      sort,
      category,
      search,
      asc: (recent || popular) ? false : asc,
      desc: (recent || popular) ? false : desc,
      recent,
      popular,
    };
    onApply(filters);
  };

  return (
    <div className="filters-box">
      <h4>Filtros</h4>
      <div className="filter-reset">
        <button className="reset-btn" onClick={resetFilters} type="button">Reiniciar filtros</button>
      </div>

      <div className="filter-row">
        <label>Buscar herramienta</label>
        <div className="search-input-wrap" style={{ position: 'relative' }}>
          <input
            type="text"
            placeholder="Buscar herramienta..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') apply();
            }}
            style={{
              width: '100%',
              padding: '8px 30px 8px 8px',
              borderRadius: '6px',
              border: '1px solid #e1e3e8',
              marginTop: '4px',
              backgroundColor: '#ffffff',
              color: '#000000'
            }}
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              style={{
                position: 'absolute',
                right: '8px',
                top: '60%',
                transform: 'translateY(-50%)',
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: '#9ca3af',
                fontSize: '18px',
                lineHeight: 1,
                padding: 0
              }}
              title="Borrar búsqueda"
            >
              &times;
            </button>
          )}
        </div>
      </div>

      <div className="filter-row">
        <label>Rango de precio</label>
        <div className="range-values">
          <span>${minPrice.toLocaleString()}</span>
          <span>${maxPrice.toLocaleString()}</span>
        </div>
        <div className="range-wrap">
          {/* Renders the minimum price slider */}
          <input
            type="range"
            min={BOUND_MIN}
            max={BOUND_MAX}
            step={STEP}
            value={minPrice}
            onChange={(e) => {
              const v = Number(e.target.value);
              if (v > maxPrice) setMinPrice(maxPrice);
              else setMinPrice(v);
            }}
          />
          {/* Renders the maximum price slider */}
          <input
            type="range"
            min={BOUND_MIN}
            max={BOUND_MAX}
            step={STEP}
            value={maxPrice}
            onChange={(e) => {
              const v = Number(e.target.value);
              if (v < minPrice) setMaxPrice(minPrice);
              else setMaxPrice(v);
            }}
          />
        </div>
      </div>

      <div className="filter-row">
        <label>Ordenar por</label>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="">Por defecto</option>
          <option value="price_asc">Precio: menor a mayor</option>
          <option value="price_desc">Precio: mayor a menor</option>
          <option value="newest">Más recientes</option>
          <option value="popular">Populares</option>
        </select>
      </div>

      <div className="filter-row">
        <label>Filtrar por Categoría</label>
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">Todas las categorías</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>{cat}</option>
          ))}
        </select>
      </div>

      <div className="filter-actions">
        <button className="td-quote" onClick={apply}>Aplicar</button>
      </div>
    </div>
  );
};

export default FiltersSidebar;
