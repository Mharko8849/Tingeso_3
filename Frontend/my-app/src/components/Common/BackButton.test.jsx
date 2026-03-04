import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import BackButton from './BackButton';

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

describe('BackButton', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('renders with default label "Volver"', () => {
    render(<BackButton />);
    expect(screen.getByText('Volver')).toBeInTheDocument();
  });

  it('renders with custom label', () => {
    render(<BackButton label="Atrás" />);
    expect(screen.getByText('Atrás')).toBeInTheDocument();
  });

  it('navigates to target path on click', () => {
    render(<BackButton to="/home" />);
    fireEvent.click(screen.getByText('Volver'));
    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  it('calls onClick callback instead of navigating when provided', () => {
    const onClick = vi.fn();
    render(<BackButton onClick={onClick} to="/home" />);
    fireEvent.click(screen.getByText('Volver'));
    expect(onClick).toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('applies custom className', () => {
    render(<BackButton className="my-cls" />);
    expect(screen.getByText('Volver').className).toContain('my-cls');
  });

  it('applies custom style', () => {
    render(<BackButton style={{ color: 'red' }} />);
    expect(screen.getByText('Volver').style.color).toBe('red');
  });

  it('has link class by default', () => {
    render(<BackButton />);
    expect(screen.getByText('Volver').className).toContain('link');
  });
});
