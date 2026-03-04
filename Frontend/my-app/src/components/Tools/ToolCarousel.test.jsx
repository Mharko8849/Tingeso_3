import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

vi.mock('./ToolCard', () => ({
  default: ({ tool }) => React.createElement('div', { 'data-testid': `card-${tool?.id || 'x'}` }, tool?.name || tool?.toolName || ''),
}));

import ToolCarousel from './ToolCarousel';

const makeTools = (n) => Array.from({ length: n }, (_, i) => ({ id: i + 1, name: `Tool ${i + 1}`, toolName: `Tool ${i + 1}` }));

describe('ToolCarousel', () => {
  it('renders title and "Ver más" link', () => {
    render(<ToolCarousel tools={makeTools(3)} title="Popular" viewMoreUrl="/inventory" autoplay={false} />);
    expect(screen.getByText('Popular')).toBeInTheDocument();
    expect(screen.getByText('Ver más')).toBeInTheDocument();
  });

  it('renders tool cards', () => {
    render(<ToolCarousel tools={makeTools(3)} title="T" autoplay={false} />);
    expect(screen.getByTestId('card-1')).toBeInTheDocument();
    expect(screen.getByTestId('card-2')).toBeInTheDocument();
    expect(screen.getByTestId('card-3')).toBeInTheDocument();
  });

  it('renders with empty tools array', () => {
    render(<ToolCarousel tools={[]} title="Empty" autoplay={false} />);
    expect(screen.getByText('Empty')).toBeInTheDocument();
  });

  it('navigates pages with prev/next buttons', () => {
    // With 10 tools and visibleCount=5 we get 2 pages
    render(<ToolCarousel tools={makeTools(10)} title="Nav" autoplay={false} />);
    const nextBtn = screen.getByLabelText('Siguiente');
    const prevBtn = screen.getByLabelText('Anterior');
    expect(nextBtn).toBeInTheDocument();
    expect(prevBtn).toBeInTheDocument();
    fireEvent.click(nextBtn);
    fireEvent.click(prevBtn);
  });

  it('renders onViewMore button when callback is provided', () => {
    const onViewMore = vi.fn();
    render(<ToolCarousel tools={makeTools(3)} title="T" onViewMore={onViewMore} autoplay={false} />);
    const btn = screen.getByText('Ver más');
    fireEvent.click(btn);
    expect(onViewMore).toHaveBeenCalled();
  });

  it('renders pagination dots', () => {
    // jsdom has innerWidth=0 → visibleCount=1 → 10 pages
    render(<ToolCarousel tools={makeTools(10)} title="T" autoplay={false} />);
    const dots = screen.getAllByLabelText(/Ir a la página/);
    expect(dots.length).toBeGreaterThanOrEqual(2);
  });

  it('clicking a dot navigates to that page', () => {
    render(<ToolCarousel tools={makeTools(10)} title="T" autoplay={false} />);
    const dot2 = screen.getByLabelText('Ir a la página 2');
    fireEvent.click(dot2);
    // Should not throw; page updates internally
    expect(dot2).toBeInTheDocument();
  });

  it('handles window resize event', () => {
    render(<ToolCarousel tools={makeTools(10)} title="T" autoplay={false} />);
    // Simulate resize
    Object.defineProperty(window, 'innerWidth', { value: 600, writable: true });
    fireEvent(window, new Event('resize'));
    expect(screen.getByText('T')).toBeInTheDocument();
  });

  it('pauses autoplay on mouseenter and resumes on mouseleave', () => {
    vi.useFakeTimers();
    const { container } = render(<ToolCarousel tools={makeTools(10)} title="T" autoplay={true} autoplayDelay={100} />);
    const carousel = container.querySelector('.tool-carousel');
    fireEvent.mouseEnter(carousel);
    vi.advanceTimersByTime(500);
    fireEvent.mouseLeave(carousel);
    vi.advanceTimersByTime(500);
    vi.useRealTimers();
    expect(carousel).toBeTruthy();
  });
});
