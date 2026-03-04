// src/components/ToolCarousel.jsx
import React, { useEffect, useState, useRef } from 'react';
import PropTypes from 'prop-types';
import ToolCard from './ToolCard';
import './ToolCarousel.css';

/**
 * ToolCarousel component.
 * Displays a carousel of ToolCards with autoplay and responsive layout.
 * 
 * Input: props (tools, title, viewMoreUrl, onViewMore, autoplay, autoplayDelay)
 * Output: JSX Element
 */
const ToolCarousel = ({
  tools = [],
  title = '',
  viewMoreUrl = '',
  onViewMore = null,
  autoplay = true,
  autoplayDelay = 3500,
}) => {
  const [page, setPage] = useState(0);
  const [visibleCount, setVisibleCount] = useState(5);

  // Navigate to previous page
  const prev = () => {
    if (tools.length === 0) return;
    setPage((p) => (p - 1 + pages) % pages);
  };

  // Navigate to next page
  const next = () => {
    if (tools.length === 0) return;
    setPage((p) => (p + 1) % pages);
  };

  // Navigate to specific page
  const goToPage = (pageIndex) => {
    if (tools.length === 0) return;
    setPage(pageIndex % pages);
  };

  // Responsive adjustment for visibleCount
  useEffect(() => {
    const compute = () => {
      const w = globalThis.innerWidth;
      if (w >= 1200) setVisibleCount(5);
      else if (w >= 992) setVisibleCount(4);
      else if (w >= 768) setVisibleCount(3);
      else if (w >= 600) setVisibleCount(2);
      else setVisibleCount(1);
    };
    compute();
    globalThis.addEventListener('resize', compute);
    return () => globalThis.removeEventListener('resize', compute);
  }, []);

  const pages = Math.max(1, Math.ceil((tools.length || 0) / visibleCount));

  // Reset page if it exceeds total pages (e.g. on resize)
  useEffect(() => {
    if (page >= pages) setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visibleCount, pages]);

  const gap = 12; // Must match CSS gap
  const cardFlexBasis = `calc((100% - ${(visibleCount - 4) * gap}px) / ${visibleCount})`;

  // Create pages array
  const pagesArr = [];
  for (let p = 0; p < pages; p++) {
    const start = p * visibleCount;
    pagesArr.push(tools.slice(start, start + visibleCount));
  }

  // Autoplay logic
  const [paused, setPaused] = useState(false);
  const AUTOPLAY_MS = autoplayDelay;
  const containerRef = useRef(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const enter = () => setPaused(true);
    const leave = () => setPaused(false);
    el.addEventListener('mouseenter', enter);
    el.addEventListener('mouseleave', leave);
    return () => {
      el.removeEventListener('mouseenter', enter);
      el.removeEventListener('mouseleave', leave);
    };
  }, []);

  useEffect(() => {
    if (!autoplay) return;
    if (paused) return;
    if (pages <= 1) return;
    const id = setInterval(() => {
      setPage((p) => (p + 1) % pages);
    }, AUTOPLAY_MS);
    return () => clearInterval(id);
  }, [paused, pages, autoplay, AUTOPLAY_MS]);

  return (
    <div
      ref={containerRef}
      className="tool-carousel container-fluid p-3 bg-light rounded shadow-sm position-relative"
    >
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">{title}</h3>
        {onViewMore ? (
          <button
            onClick={onViewMore}
            className="primary-cta"
            style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
          >
            Ver más
          </button>
        ) : (
          <a href={viewMoreUrl} className="view-more-btn">Ver más</a>
        )}
      </div>

      {/* Left Arrow */}
      <button onClick={prev} className="carousel-btn left btn btn-light shadow-sm" aria-label="Anterior">
        <span aria-hidden>‹</span>
      </button>

      {/* Carousel Track */}
      <div className="carousel-viewport overflow-hidden">
        <div
          className="carousel-track d-flex"
          style={{ width: `${pages * 100}%`, transform: `translateX(-${page * (100 / pages)}%)` }}
        >
          {pagesArr.map((pageTools) => {
            const pageKey = pageTools.map(t => t?.id).filter(Boolean).join('-') || 'empty';
            return (
            <div className="carousel-page d-flex" key={pageKey} style={{ width: `${100 / pages}%` }}>
              {pageTools.map((tool) => (
                <ToolCard key={String(tool?.id ?? tool?.toolName)} tool={tool} layout="vertical" style={{ flex: `0 0 ${cardFlexBasis}` }} />
              ))}
            </div>
            );
          })}
        </div>
      </div>

      {/* Right Arrow */}
      <button onClick={next} className="carousel-btn right btn btn-light shadow-sm" aria-label="Siguiente">
        <span aria-hidden>›</span>
      </button>

      {/* Pagination Dots */}
      <div className="d-flex justify-content-center mt-3">
        {Array.from({ length: pages }, (_, i) => i + 1).map((pageNum) => (
          <button
            key={`dot-page-${pageNum}`}
            className={`dot-btn ${pageNum - 1 === page ? 'active' : ''}`}
            onClick={() => goToPage(pageNum - 1)}
            aria-label={`Ir a la página ${pageNum}`}
          />
        ))}
      </div>
    </div>
  );
};

ToolCarousel.propTypes = {
  autoplay: PropTypes.string,
  autoplayDelay: PropTypes.string,
  onViewMore: PropTypes.func,
  title: PropTypes.string,
  tools: PropTypes.array,
  viewMoreUrl: PropTypes.string,
};

export default ToolCarousel;
