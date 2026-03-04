import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.fn();
vi.mock('../services/http-common', () => ({ default: { get: (...a) => mockGet(...a), post: vi.fn(), put: vi.fn() } }));
vi.mock('../components/Layout/NavBar', () => ({ default: () => React.createElement('nav', { 'data-testid': 'navbar' }) }));
vi.mock('../components/Common/BackButton', () => ({ default: ({ onClick }) => React.createElement('button', { onClick, 'data-testid': 'back' }, 'Back') }));
vi.mock('../components/Loading/LoadingSpinner', () => ({ default: ({ message }) => React.createElement('div', null, message) }));
vi.mock('../components/Badges/Badge', () => ({ default: ({ title }) => React.createElement('span', null, title) }));
vi.mock('../components/Stock/ModalAddStockTool', () => ({ default: () => null }));
vi.mock('../components/Stock/ModalAddNewTool', () => ({ default: () => null }));
vi.mock('../components/Stock/ModalEditTool', () => ({ default: () => null }));
vi.mock('../components/Alerts/AlertContext', () => ({
  useAlert: () => ({ showAlert: vi.fn(), show: vi.fn() }),
}));
vi.mock('@react-keycloak/web', () => ({
  useKeycloak: () => ({ keycloak: { authenticated: true, initialized: true, tokenParsed: { realm_access: { roles: ['ADMIN'] } } }, initialized: true }),
}));
vi.mock('../services/auth', () => ({
  getUser: () => ({ realm_access: { roles: ['ADMIN'] } }),
}));

import ToolDetail from './ToolDetail';

describe('ToolDetail', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('renders loading state when fetching', () => {
    mockGet.mockReturnValue(new Promise(() => {}));
    render(<ToolDetail id="5" />);
    expect(screen.getByText('Cargando herramienta...')).toBeInTheDocument();
  });

  it('renders tool details after loading', async () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/inventory/filter') {
        return Promise.resolve({
          data: [{
            idTool: { id: 5, toolName: 'Power Drill', priceRent: 10000, category: 'Power Tools', imageUrl: 'drill.jpg', repoCost: 50000, priceFineAtDate: 500 },
            toolState: { state: 'DISPONIBLE', color: '#00ff00' },
            stockTool: 3,
          }],
        });
      }
      if (url === '/api/tool-states/') {
        return Promise.resolve({ data: [{ state: 'DISPONIBLE', color: '#00ff00' }, { state: 'PRESTADA', color: '#ff0000' }] });
      }
      return Promise.resolve({ data: {} });
    });
    render(<ToolDetail id="5" />);
    await waitFor(() => {
      expect(screen.getByText('Power Drill')).toBeInTheDocument();
      expect(screen.getByText(`$${(10000).toLocaleString()}`)).toBeInTheDocument();
    });
  });

  it('renders error when tool not found', async () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/inventory/filter') return Promise.resolve({ data: [] });
      if (url === '/api/tool-states/') return Promise.resolve({ data: [] });
      return Promise.resolve({ data: {} });
    });
    render(<ToolDetail id="999" />);
    await waitFor(() => {
      expect(screen.getByText('Herramienta no encontrada')).toBeInTheDocument();
    });
  });

  it('renders error on API failure', async () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/inventory/filter') return Promise.reject(new Error('fail'));
      if (url === '/api/tool-states/') return Promise.resolve({ data: [] });
      return Promise.resolve({ data: {} });
    });
    render(<ToolDetail id="5" />);
    await waitFor(() => {
      expect(screen.getByText('No se pudo cargar la herramienta')).toBeInTheDocument();
    });
  });

  it('renders without id prop', () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/tool-states/') return Promise.resolve({ data: [] });
      return Promise.resolve({ data: [] });
    });
    const { container } = render(<ToolDetail />);
    expect(container).toBeTruthy();
  });

  it('shows admin buttons for ADMIN roles', async () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/inventory/filter') {
        return Promise.resolve({
          data: [{ idTool: { id: 5, toolName: 'X', priceRent: 100 }, toolState: { state: 'DISPONIBLE' }, stockTool: 1 }],
        });
      }
      if (url === '/api/tool-states/') return Promise.resolve({ data: [{ state: 'DISPONIBLE', color: '#0f0' }] });
      return Promise.resolve({ data: {} });
    });
    render(<ToolDetail id="5" />);
    await waitFor(() => {
      expect(screen.getByText('Añadir Stock')).toBeInTheDocument();
      expect(screen.getByText('Editar Producto')).toBeInTheDocument();
    });
  });

  it('handles tool with category as object', async () => {
    mockGet.mockImplementation((url) => {
      if (url === '/api/inventory/filter') {
        return Promise.resolve({
          data: [{ idTool: { id: 5, toolName: 'Y', priceRent: 200, category: { name: 'Cat1' } }, toolState: { state: 'DISPONIBLE' }, stockTool: 1 }],
        });
      }
      if (url === '/api/tool-states/') return Promise.resolve({ data: [{ state: 'DISPONIBLE', color: '#0f0' }] });
      return Promise.resolve({ data: {} });
    });
    render(<ToolDetail id="5" />);
    await waitFor(() => {
      expect(screen.getByText('Cat1')).toBeInTheDocument();
    });
  });
});
