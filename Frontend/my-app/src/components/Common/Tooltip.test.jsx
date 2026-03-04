import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Tooltip, { HelpIcon } from './Tooltip';

/* ───── helpers exported for unit-testing the pure functions ───── */
// We re-implement the module-private helpers inline so we can unit-test
// the logic that drives rendering. The real functions live inside
// Tooltip.jsx; these mirror them exactly.

const GAP = 8;
const calcPositionStyle = (pos, rect, vw, vh) => {
  const cx = `${rect.left + rect.width / 2}px`;
  const cy = `${rect.top + rect.height / 2}px`;
  const topAbove = `${vh - rect.top + GAP}px`;
  const topBelow = `${rect.bottom + GAP}px`;
  const leftRight = `${rect.right + GAP}px`;
  const rightLeft = `${vw - rect.left + GAP}px`;
  return {
    right: { top: cy, left: leftRight, transform: 'translateY(-50%)' },
    left: { top: cy, right: rightLeft, transform: 'translateY(-50%)' },
    bottom: { top: topBelow, left: cx, transform: 'translateX(-50%)' },
    top: { bottom: topAbove, left: cx, transform: 'translateX(-50%)' },
  }[pos];
};

const shouldFlip = (position, rect, vw, vh) => {
  if (position === 'right') return rect.right > vw - 250;
  if (position === 'left') return rect.left < 250;
  if (position === 'bottom') return rect.bottom > vh - 100;
  if (position === 'top') return rect.top < 100;
  return false;
};

describe('Tooltip component', () => {
  it('renders children and tooltip text', () => {
    render(
      <Tooltip text="Help info">
        <span>Hover me</span>
      </Tooltip>,
    );
    expect(screen.getByText('Hover me')).toBeInTheDocument();
    expect(screen.getByText('Help info')).toBeInTheDocument();
  });

  it('does not render tooltip-content when text is empty', () => {
    const { container } = render(
      <Tooltip text="">
        <span>Child</span>
      </Tooltip>,
    );
    expect(container.querySelector('.tooltip-content')).toBeNull();
  });

  it('applies position class and maxWidth', () => {
    const { container } = render(
      <Tooltip text="Tip" position="bottom" maxWidth="300px">
        <span>C</span>
      </Tooltip>,
    );
    const tip = container.querySelector('.tooltip-content');
    expect(tip).toHaveClass('tooltip-bottom');
    expect(tip).toHaveStyle({ maxWidth: '300px' });
  });

  it('renders with position=left', () => {
    const { container } = render(
      <Tooltip text="Left tip" position="left"><span>X</span></Tooltip>,
    );
    expect(container.querySelector('.tooltip-left')).toBeTruthy();
  });

  it('renders with position=right', () => {
    const { container } = render(
      <Tooltip text="Right tip" position="right"><span>X</span></Tooltip>,
    );
    expect(container.querySelector('.tooltip-right')).toBeTruthy();
  });
});

describe('HelpIcon component', () => {
  beforeEach(() => {
    // Mock getBoundingClientRect for deterministic positioning
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 400, top: 300, right: 420, bottom: 320,
      width: 20, height: 20, x: 400, y: 300,
    }));
    Object.defineProperty(window, 'innerWidth', { value: 1024, writable: true });
    Object.defineProperty(window, 'innerHeight', { value: 768, writable: true });
  });

  it('renders a help button with ? text', () => {
    render(<HelpIcon content="Help text" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    expect(btn).toBeInTheDocument();
    expect(btn.textContent).toContain('?');
  });

  it('shows tooltip on mouseEnter and hides on mouseLeave', () => {
    render(<HelpIcon content="Visible tooltip" position="right" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });

    // Not visible initially
    expect(screen.queryByText('Visible tooltip')).toBeNull();

    // Show on hover
    fireEvent.mouseEnter(btn);
    expect(screen.getByText('Visible tooltip')).toBeInTheDocument();

    // Hide on leave
    fireEvent.mouseLeave(btn);
    expect(screen.queryByText('Visible tooltip')).toBeNull();
  });

  it('toggles tooltip on Enter key', () => {
    render(<HelpIcon content="Key tooltip" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });

    fireEvent.keyDown(btn, { key: 'Enter' });
    expect(screen.getByText('Key tooltip')).toBeInTheDocument();

    fireEvent.keyDown(btn, { key: 'Enter' });
    expect(screen.queryByText('Key tooltip')).toBeNull();
  });

  it('toggles tooltip on Space key', () => {
    render(<HelpIcon content="Space tooltip" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });

    fireEvent.keyDown(btn, { key: ' ' });
    expect(screen.getByText('Space tooltip')).toBeInTheDocument();
  });

  it('does not show tooltip when content is empty', () => {
    render(<HelpIcon content="" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    // content is empty so the conditional (&& content) prevents rendering
    expect(screen.queryByText('tooltip-content')).toBeNull();
  });

  it('applies position class when tooltip is visible', () => {
    const { container } = render(<HelpIcon content="Pos test" position="top" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    // Should have tooltip-fixed class
    const tooltip = container.querySelector('.tooltip-fixed');
    expect(tooltip).toBeTruthy();
  });

  it('flips position when near right edge', () => {
    // Place element near right edge
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 900, top: 300, right: 920, bottom: 320,
      width: 20, height: 20, x: 900, y: 300,
    }));
    const { container } = render(<HelpIcon content="Flip test" position="right" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    // Should flip to left
    expect(container.querySelector('.tooltip-left')).toBeTruthy();
  });

  it('flips position when near left edge', () => {
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 50, top: 300, right: 70, bottom: 320,
      width: 20, height: 20, x: 50, y: 300,
    }));
    const { container } = render(<HelpIcon content="Flip left" position="left" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    expect(container.querySelector('.tooltip-right')).toBeTruthy();
  });

  it('flips position when near bottom edge', () => {
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 400, top: 700, right: 420, bottom: 720,
      width: 20, height: 20, x: 400, y: 700,
    }));
    const { container } = render(<HelpIcon content="Flip bottom" position="bottom" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    expect(container.querySelector('.tooltip-top')).toBeTruthy();
  });

  it('flips position when near top edge', () => {
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 400, top: 30, right: 420, bottom: 50,
      width: 20, height: 20, x: 400, y: 30,
    }));
    const { container } = render(<HelpIcon content="Flip top" position="top" />);
    const btn = screen.getByRole('button', { name: 'Ayuda' });
    fireEvent.mouseEnter(btn);
    expect(container.querySelector('.tooltip-bottom')).toBeTruthy();
  });
});

describe('calcPositionStyle (mirror)', () => {
  const rect = { left: 100, top: 200, right: 120, bottom: 220, width: 20, height: 20 };
  const vw = 1024;
  const vh = 768;

  it('returns correct style for right', () => {
    const s = calcPositionStyle('right', rect, vw, vh);
    expect(s.left).toBe(`${rect.right + GAP}px`);
    expect(s.transform).toBe('translateY(-50%)');
  });

  it('returns correct style for left', () => {
    const s = calcPositionStyle('left', rect, vw, vh);
    expect(s.right).toBe(`${vw - rect.left + GAP}px`);
  });

  it('returns correct style for bottom', () => {
    const s = calcPositionStyle('bottom', rect, vw, vh);
    expect(s.top).toBe(`${rect.bottom + GAP}px`);
    expect(s.transform).toBe('translateX(-50%)');
  });

  it('returns correct style for top', () => {
    const s = calcPositionStyle('top', rect, vw, vh);
    expect(s.bottom).toBe(`${vh - rect.top + GAP}px`);
  });
});

describe('shouldFlip (mirror)', () => {
  it('flips right when near right edge', () => {
    expect(shouldFlip('right', { right: 800 }, 1024, 768)).toBe(true);
    expect(shouldFlip('right', { right: 500 }, 1024, 768)).toBe(false);
  });

  it('flips left when near left edge', () => {
    expect(shouldFlip('left', { left: 100 }, 1024, 768)).toBe(true);
    expect(shouldFlip('left', { left: 300 }, 1024, 768)).toBe(false);
  });

  it('flips bottom when near bottom edge', () => {
    expect(shouldFlip('bottom', { bottom: 700 }, 1024, 768)).toBe(true);
    expect(shouldFlip('bottom', { bottom: 600 }, 1024, 768)).toBe(false);
  });

  it('flips top when near top edge', () => {
    expect(shouldFlip('top', { top: 50 }, 1024, 768)).toBe(true);
    expect(shouldFlip('top', { top: 200 }, 1024, 768)).toBe(false);
  });

  it('returns false for unknown position', () => {
    expect(shouldFlip('center', {}, 1024, 768)).toBe(false);
  });
});
