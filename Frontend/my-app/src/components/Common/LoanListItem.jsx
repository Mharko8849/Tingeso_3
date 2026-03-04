import React from 'react';
import PropTypes from 'prop-types';
import Badge from '../Badges/Badge';
import { statusToBadgeVariant } from '../Badges/statusToBadge';

const loanItemStyle = {
  all: 'unset',
  width: '100%',
  padding: 14,
  borderRadius: 8,
  border: '1px solid #e6e6e6',
  background: '#fff',
  cursor: 'pointer',
  display: 'flex',
  alignItems: 'center',
  gap: 24,
};

const headerStyle = { fontSize: 13, fontWeight: 700, color: '#374151' };
const cellStyle = { fontSize: 14, color: '#374151' };

/**
 * Reusable loan list item button used in Loans and ReturnsClientLoans pages.
 */
const LoanListItem = ({ loan, columns, gridTemplate, onClick }) => (
  <button type="button" onClick={onClick} style={loanItemStyle}>
    <div style={{ display: 'grid', gridTemplateColumns: gridTemplate, alignItems: 'center', gap: 12, width: '100%' }}>
      <div style={{ fontWeight: 800, fontSize: 16 }}>#{loan.id}</div>
      {columns.map(({ key, render }) => (
        <div key={key} style={cellStyle}>{render ? render(loan) : loan[key]}</div>
      ))}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Badge variant={statusToBadgeVariant(loan.status)} title={loan.status || ''} />
        <div style={cellStyle}>{loan.status}</div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <span className="link" style={{ whiteSpace: 'nowrap' }}>Ver pedido</span>
      </div>
    </div>
  </button>
);

LoanListItem.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.shape({
    key: PropTypes.string.isRequired,
    render: PropTypes.func,
  })).isRequired,
  gridTemplate: PropTypes.string.isRequired,
  loan: PropTypes.object.isRequired,
  onClick: PropTypes.func.isRequired,
};

/**
 * Reusable loan list header row.
 */
const LoanListHeader = ({ headers, gridTemplate }) => (
  <div style={{ display: 'grid', gridTemplateColumns: gridTemplate, gap: 12, padding: '6px 8px', borderBottom: '1px solid #f1f5f9', marginBottom: 8, alignItems: 'center' }}>
    {headers.map((h) => (
      <div key={h} style={{ ...headerStyle, ...(h === 'Acciones' ? { textAlign: 'right' } : {}) }}>{h}</div>
    ))}
  </div>
);

LoanListHeader.propTypes = {
  gridTemplate: PropTypes.string.isRequired,
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export { LoanListItem, LoanListHeader, loanItemStyle, headerStyle, cellStyle };
