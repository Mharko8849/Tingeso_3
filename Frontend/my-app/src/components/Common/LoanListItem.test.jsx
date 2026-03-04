import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LoanListItem, LoanListHeader } from './LoanListItem';

// Mock Badge component
vi.mock('../Badges/Badge', () => ({
  default: ({ variant, title }) => <span data-testid="badge" data-variant={variant}>{title}</span>,
}));

vi.mock('../Badges/statusToBadge', () => ({
  statusToBadgeVariant: (s) => s === 'ACTIVO' ? 'green' : 'blue',
}));

describe('LoanListHeader', () => {
  it('renders all header labels', () => {
    render(
      <LoanListHeader
        gridTemplate="80px 1fr 160px 120px"
        headers={['Pedido #', 'Fecha', 'Estado', 'Acciones']}
      />,
    );
    expect(screen.getByText('Pedido #')).toBeInTheDocument();
    expect(screen.getByText('Fecha')).toBeInTheDocument();
    expect(screen.getByText('Estado')).toBeInTheDocument();
    expect(screen.getByText('Acciones')).toBeInTheDocument();
  });
});

describe('LoanListItem', () => {
  const mockLoan = {
    id: 42,
    initDate: '2025-01-01',
    returnDate: '2025-02-01',
    status: 'ACTIVO',
  };

  it('renders loan id', () => {
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 1fr 1fr 160px 120px"
        columns={[
          { key: 'initDate' },
          { key: 'returnDate' },
        ]}
        onClick={() => {}}
      />,
    );
    expect(screen.getByText('#42')).toBeInTheDocument();
  });

  it('renders columns with render functions', () => {
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 1fr 160px 120px"
        columns={[
          { key: 'initDate', render: (l) => `Formatted: ${l.initDate}` },
        ]}
        onClick={() => {}}
      />,
    );
    expect(screen.getByText('Formatted: 2025-01-01')).toBeInTheDocument();
  });

  it('renders raw value when no render function', () => {
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 1fr 160px 120px"
        columns={[{ key: 'initDate' }]}
        onClick={() => {}}
      />,
    );
    expect(screen.getByText('2025-01-01')).toBeInTheDocument();
  });

  it('renders status badge', () => {
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 1fr 160px 120px"
        columns={[]}
        onClick={() => {}}
      />,
    );
    expect(screen.getByTestId('badge')).toBeInTheDocument();
    // Status text appears in a sibling div
    expect(screen.getAllByText('ACTIVO').length).toBeGreaterThanOrEqual(1);
  });

  it('calls onClick when clicked', () => {
    const onClick = vi.fn();
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 160px 120px"
        columns={[]}
        onClick={onClick}
      />,
    );
    fireEvent.click(screen.getByRole('button'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('renders "Ver pedido" link text', () => {
    render(
      <LoanListItem
        loan={mockLoan}
        gridTemplate="80px 160px 120px"
        columns={[]}
        onClick={() => {}}
      />,
    );
    expect(screen.getByText('Ver pedido')).toBeInTheDocument();
  });
});
