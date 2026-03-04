import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import LoadingSpinner from './LoadingSpinner';

describe('LoadingSpinner', () => {
  it('renders with default message', () => {
    render(<LoadingSpinner />);
    expect(screen.getByText('Cargando...')).toBeInTheDocument();
  });

  it('renders custom message', () => {
    render(<LoadingSpinner message="Procesando..." />);
    expect(screen.getByText('Procesando...')).toBeInTheDocument();
  });

  it('does not render message when empty', () => {
    render(<LoadingSpinner message="" />);
    expect(screen.queryByText('Cargando...')).not.toBeInTheDocument();
  });

  it('renders in inline mode by default', () => {
    const { container } = render(<LoadingSpinner />);
    expect(container.querySelector('.loading-inline')).toBeInTheDocument();
    expect(container.querySelector('.loading-overlay')).not.toBeInTheDocument();
  });

  it('renders in fullScreen mode', () => {
    const { container } = render(<LoadingSpinner fullScreen={true} />);
    expect(container.querySelector('.loading-overlay')).toBeInTheDocument();
  });

  it('applies size class', () => {
    const { container } = render(<LoadingSpinner size="large" />);
    expect(container.querySelector('.loading-spinner-large')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(<LoadingSpinner className="my-class" />);
    expect(container.querySelector('.my-class')).toBeInTheDocument();
  });

  it('fullScreen mode shows message', () => {
    render(<LoadingSpinner fullScreen message="Loading full" />);
    expect(screen.getByText('Loading full')).toBeInTheDocument();
  });
});
