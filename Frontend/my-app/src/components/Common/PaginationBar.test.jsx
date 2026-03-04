import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import PaginationBar from './PaginationBar';

describe('PaginationBar', () => {
  it('renders page indicator', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={20} onPageChange={() => {}} />,
    );
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });

  it('shows summary when showSummary=true', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={20} showSummary={true} onPageChange={() => {}} />,
    );
    expect(screen.getByText(/Mostrando 8 de 20/)).toBeInTheDocument();
  });

  it('hides summary when showSummary=false', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={20} showSummary={false} onPageChange={() => {}} />,
    );
    expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument();
  });

  it('shows page size controls when showPageSizeControls=true', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={20} showPageSizeControls={true} onPageChange={() => {}} />,
    );
    expect(screen.getByLabelText('Mostrar por página:')).toBeInTheDocument();
  });

  it('hides page size controls when showPageSizeControls=false', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={20} showPageSizeControls={false} onPageChange={() => {}} />,
    );
    expect(screen.queryByLabelText('Mostrar por página:')).not.toBeInTheDocument();
  });

  it('calls onPageChange when clicking next/prev buttons', () => {
    const onPageChange = vi.fn();
    render(
      <PaginationBar page={2} pageSize={8} total={24} onPageChange={onPageChange} />,
    );
    // Click "next" button
    const buttons = screen.getAllByRole('button');
    // First button: |< (go to first)
    // Second button: < (go prev)
    // Third button: > (go next)
    // Fourth button: >| (go last)
    fireEvent.click(buttons[0]); // |< go to page 1
    expect(onPageChange).toHaveBeenCalledWith(1);

    fireEvent.click(buttons[1]); // < go prev (page 1)
    expect(onPageChange).toHaveBeenCalledWith(1);

    fireEvent.click(buttons[2]); // > go next (page 3)
    expect(onPageChange).toHaveBeenCalledWith(3);

    fireEvent.click(buttons[3]); // >| go last (page 3)
    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  it('disables prev buttons on first page', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={24} onPageChange={() => {}} />,
    );
    const buttons = screen.getAllByRole('button');
    expect(buttons[0]).toBeDisabled(); // |<
    expect(buttons[1]).toBeDisabled(); // <
    expect(buttons[2]).not.toBeDisabled(); // >
  });

  it('disables next buttons on last page', () => {
    render(
      <PaginationBar page={3} pageSize={8} total={24} onPageChange={() => {}} />,
    );
    const buttons = screen.getAllByRole('button');
    expect(buttons[2]).toBeDisabled(); // >
    expect(buttons[3]).toBeDisabled(); // >|
  });

  it('handles zero total', () => {
    render(
      <PaginationBar page={1} pageSize={8} total={0} showSummary={true} onPageChange={() => {}} />,
    );
    expect(screen.getByText(/Mostrando 0 de 0/)).toBeInTheDocument();
  });

  it('calls onPageSizeChange when selecting a new page size', () => {
    const onPageSizeChange = vi.fn();
    render(
      <PaginationBar page={1} pageSize={8} total={20} showPageSizeControls={true} onPageChange={() => {}} onPageSizeChange={onPageSizeChange} />,
    );
    fireEvent.change(screen.getByLabelText('Mostrar por página:'), { target: { value: '20' } });
    expect(onPageSizeChange).toHaveBeenCalledWith(20);
  });
});
