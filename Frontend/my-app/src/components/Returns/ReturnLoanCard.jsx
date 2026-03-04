import React from 'react';
import PropTypes from 'prop-types';

const ReturnLoanCard = ({ loan, onClick }) => (
    <li className="card"><button type="button" onClick={() => onClick(loan.id)} style={{ all: 'unset', display: 'block', width: '100%', cursor: 'pointer', textAlign: 'left' }}>
      <strong>Pedido #{loan.id}</strong>
      <div>Fecha inicio: {loan.initDate}</div>
      <div>Fecha devolución: {loan.returnDate}</div>
      <div>Estado: {loan.status}</div>
    </button></li>
  );

ReturnLoanCard.propTypes = {
  loan: PropTypes.object,
  onClick: PropTypes.func,
};

export default ReturnLoanCard;
