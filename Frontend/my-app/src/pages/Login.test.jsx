import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';

const mockNavigate = vi.fn();
const mockShow = vi.fn();

vi.mock('../services/http-common', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));
vi.mock('../components/Alerts/useAlert', () => ({
  useAlert: () => ({ show: mockShow }),
}));
vi.mock('../components/Alerts/AlertContext', () => ({
  AlertContext: React.createContext({ show: vi.fn() }),
  showGlobalAlert: vi.fn(),
}));
vi.mock('../components/Layout/NavBar', () => ({
  default: () => React.createElement('nav', { 'data-testid': 'navbar' }),
}));

import Login from './Login';
import api from '../services/http-common';

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders login form', () => {
    render(React.createElement(Login));
    expect(screen.getByText('Iniciar sesión')).toBeInTheDocument();
    expect(screen.getByLabelText(/Usuario o Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Contraseña/i)).toBeInTheDocument();
  });

  it('renders submit and register buttons', () => {
    render(React.createElement(Login));
    expect(screen.getByText('Ingresar')).toBeInTheDocument();
    expect(screen.getByText('Crear cuenta')).toBeInTheDocument();
  });

  it('allows typing in identifier and password fields', () => {
    render(React.createElement(Login));
    const identifier = screen.getByLabelText(/Usuario o Email/i);
    const password = screen.getByLabelText(/Contraseña/i);

    fireEvent.change(identifier, { target: { value: 'testuser' } });
    fireEvent.change(password, { target: { value: 'pass123' } });

    expect(identifier.value).toBe('testuser');
    expect(password.value).toBe('pass123');
  });

  it('submits form and stores auth data with access_token', async () => {
    api.post.mockResolvedValueOnce({
      data: {
        access_token: 'abc123',
        refresh_token: 'ref456',
        user: { name: 'Juan', id: 1 },
      },
    });

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'juan' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'password' } });
    fireEvent.click(screen.getByText('Ingresar'));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/auth/login', { username: 'juan', password: 'password' });
      expect(localStorage.getItem('access_token')).toBe('abc123');
      expect(localStorage.getItem('refresh_token')).toBe('ref456');
      expect(localStorage.getItem('user')).toContain('Juan');
    });
  });

  it('stores auth data with token object format', async () => {
    api.post.mockResolvedValueOnce({
      data: {
        token: { access_token: 'tok1', refresh_token: 'ref1' },
        user: { name: 'Maria' },
      },
    });

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'maria' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByText('Ingresar'));

    await waitFor(() => {
      expect(localStorage.getItem('access_token')).toBe('tok1');
      expect(localStorage.getItem('app_token')).toBe('tok1');
    });
  });

  it('shows error message on 401', async () => {
    api.post.mockRejectedValueOnce({ response: { status: 401 } });

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'user' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByText('Ingresar'));

    await waitFor(() => {
      expect(screen.getByText(/Credenciales incorrectas/)).toBeInTheDocument();
    });
  });

  it('shows error message on 404', async () => {
    api.post.mockRejectedValueOnce({ response: { status: 404 } });

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'nouser' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByText('Ingresar'));

    await waitFor(() => {
      expect(screen.getByText(/no se encuentra registrado/i)).toBeInTheDocument();
    });
  });

  it('shows generic error on server error', async () => {
    api.post.mockRejectedValueOnce({ response: { status: 500 } });

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'user' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByText('Ingresar'));

    await waitFor(() => {
      expect(screen.getByText(/error al iniciar sesión/i)).toBeInTheDocument();
    });
  });

  it('navigates to register page when clicking Crear cuenta', () => {
    render(React.createElement(Login));
    fireEvent.click(screen.getByText('Crear cuenta'));
    expect(mockNavigate).toHaveBeenCalledWith('/register');
  });

  it('shows loading state on submit', async () => {
    let resolvePost;
    api.post.mockImplementationOnce(() => new Promise((resolve) => { resolvePost = resolve; }));

    render(React.createElement(Login));
    fireEvent.change(screen.getByLabelText(/Usuario o Email/i), { target: { value: 'user' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByText('Ingresar'));

    expect(screen.getByText('Entrando...')).toBeInTheDocument();

    resolvePost({ data: { access_token: 'x' } });
    await waitFor(() => {
      expect(screen.queryByText('Entrando...')).not.toBeInTheDocument();
    });
  });
});
