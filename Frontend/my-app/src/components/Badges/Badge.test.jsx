import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Badge from './Badge';

describe('Badge', () => {
  it('renders with default variant (red)', () => {
    render(<Badge />);
    const badge = screen.getByLabelText('Inactivo');
    expect(badge).toBeInTheDocument();
    expect(badge.tagName).toBe('SPAN');
  });

  it('renders green variant with correct aria-label', () => {
    render(<Badge variant="green" />);
    expect(screen.getByLabelText('Activo')).toBeInTheDocument();
  });

  it('uses title as aria-label when provided', () => {
    render(<Badge variant="blue" title="FINALIZADO" />);
    expect(screen.getByLabelText('FINALIZADO')).toBeInTheDocument();
  });

  it('uses custom ariaLabel when provided', () => {
    render(<Badge variant="yellow" ariaLabel="Custom label" />);
    expect(screen.getByLabelText('Custom label')).toBeInTheDocument();
  });

  it('applies CSS classes when useClasses is true (default)', () => {
    render(<Badge variant="green" />);
    const badge = screen.getByLabelText('Activo');
    expect(badge.className).toContain('badge');
    expect(badge.className).toContain('badge--green');
  });

  it('does not apply CSS classes when useClasses is false', () => {
    render(<Badge variant="green" useClasses={false} />);
    const badge = screen.getByLabelText('Activo');
    expect(badge.className).toBe('');
  });

  it('applies custom className', () => {
    render(<Badge variant="red" className="custom-class" />);
    const badge = screen.getByLabelText('Inactivo');
    expect(badge.className).toContain('custom-class');
  });

  it('applies custom inline style', () => {
    render(<Badge variant="blue" title="test" style={{ opacity: 0.5 }} />);
    const badge = screen.getByLabelText('test');
    expect(badge.style.opacity).toBe('0.5');
  });

  it('renders title attribute', () => {
    render(<Badge title="Estado actual" />);
    expect(screen.getByTitle('Estado actual')).toBeInTheDocument();
  });

  it('defaults aria-label to "Estado" for non-green non-red variants without title', () => {
    render(<Badge variant="yellow" />);
    expect(screen.getByLabelText('Estado')).toBeInTheDocument();
  });
});
