import React from 'react';
import PropTypes from 'prop-types';

const ReturnClientCard = ({ client, onClick }) => (
    <li className="card"><button type="button" onClick={() => onClick(client.id)} style={{ all: 'unset', display: 'block', width: '100%', cursor: 'pointer', textAlign: 'left' }}>
      <strong>{client.name ? `${client.name} ${client.lastName || ''}` : (client.username || client.email || `Cliente ${client.id}`)}</strong>
      <div style={{ fontSize: 13, color: '#666' }}>{client.username || client.email}</div>
      <div>ID: {client.id}</div>
    </button></li>
  );

ReturnClientCard.propTypes = {
  client: PropTypes.object,
  onClick: PropTypes.func,
};

export default ReturnClientCard;
