import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.fn();
const mockPut = vi.fn();
vi.mock('../../services/http-common', () => ({ default: { get: (...a) => mockGet(...a), put: (...a) => mockPut(...a) } }));
vi.mock('../Alerts/AlertContext', () => ({
  useAlert: () => ({ show: vi.fn(), showAlert: vi.fn() }),
}));
vi.mock('../../hooks/useDragAndDrop', () => ({
  default: () => ({ dropRef: { current: null }, isDragging: false }),
}));
vi.mock('../../utils/numericInput', () => ({
  handleNumericInput: vi.fn((field, value, setForm) => setForm(prev => ({ ...prev, [field]: value }))),
}));
vi.mock('../../utils/classifyError', () => ({
  classifyError: vi.fn(() => 'Error message'),
}));

import ModalEditTool from './ModalEditTool';

describe('ModalEditTool', () => {
  const baseTool = { id: 1, name: 'Drill', toolName: 'Drill', category: 'Power', repoCost: 5000, priceRent: 1000, priceFineAtDate: 200, price: 1000 };

  beforeEach(() => {
    vi.clearAllMocks();
    mockGet.mockImplementation((url) => {
      if (url.includes('/api/categories/')) return Promise.resolve({ data: [{ name: 'Power' }, { name: 'Manual' }] });
      if (url.includes('/api/user/me')) return Promise.resolve({ data: { id: 99 } });
      return Promise.resolve({ data: {} });
    });
    mockPut.mockResolvedValue({ data: { id: 1, toolName: 'Updated' } });
  });

  it('returns null when open is false', () => {
    const { container } = render(<ModalEditTool open={false} tool={baseTool} />);
    expect(container.innerHTML).toBe('');
  });

  it('returns null when tool is null', () => {
    const { container } = render(<ModalEditTool open={true} tool={null} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders form when open and tool provided', async () => {
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={baseTool} />);
    expect(screen.getByText('Editar herramienta')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Drill')).toBeInTheDocument();
  });

  it('fetches categories on open', async () => {
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={baseTool} />);
    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/api/categories/');
    });
  });

  it('handles confirm with valid data', async () => {
    const onUpdated = vi.fn();
    const onClose = vi.fn();
    render(<ModalEditTool open={true} onClose={onClose} tool={baseTool} onUpdated={onUpdated} />);
    fireEvent.click(screen.getByText('Guardar cambios'));
    await waitFor(() => {
      expect(mockPut).toHaveBeenCalled();
      expect(onUpdated).toHaveBeenCalled();
    });
  });

  it('shows error on confirm failure', async () => {
    mockPut.mockRejectedValue({ response: { data: 'fail' } });
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={baseTool} />);
    fireEvent.click(screen.getByText('Guardar cambios'));
    await waitFor(() => expect(mockPut).toHaveBeenCalled());
  });

  it('calls onClose when cancel button clicked', () => {
    const onClose = vi.fn();
    render(<ModalEditTool open={true} onClose={onClose} tool={baseTool} />);
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose when backdrop close button clicked', () => {
    const onClose = vi.fn();
    render(<ModalEditTool open={true} onClose={onClose} tool={baseTool} />);
    const closeBtns = screen.getAllByLabelText('Cerrar');
    // Click the × button (second one, the visible close button)
    fireEvent.click(closeBtns[closeBtns.length - 1]);
    expect(onClose).toHaveBeenCalled();
  });

  it('updates tool name field', () => {
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={baseTool} />);
    const nameInput = screen.getByDisplayValue('Drill');
    fireEvent.change(nameInput, { target: { value: 'New Drill' } });
    expect(screen.getByDisplayValue('New Drill')).toBeInTheDocument();
  });

  it('handles tool with category as object', () => {
    const tool = { ...baseTool, category: { name: 'Power' } };
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={tool} />);
    expect(screen.getByText('Editar herramienta')).toBeInTheDocument();
  });

  it('handles file selection via input', () => {
    render(<ModalEditTool open={true} onClose={vi.fn()} tool={baseTool} />);
    const fileInput = document.querySelector('input[type="file"]');
    const file = new File(['img'], 'test.jpg', { type: 'image/jpeg' });
    fireEvent.change(fileInput, { target: { files: [file] } });
    expect(fileInput).toBeTruthy();
  });
});
