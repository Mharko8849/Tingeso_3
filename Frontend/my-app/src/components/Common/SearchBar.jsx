import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * A simple search bar component.
 * Allows searching by text and redirects to inventory.
 *
 * @returns {JSX.Element} - The search bar input.
 */
const SearchBar = () => {
  const [term, setTerm] = useState('');
  const navigate = useNavigate();

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      // Redirect to inventory with search param
      if (term.trim()) {
        navigate(`/inventory?search=${encodeURIComponent(term.trim())}`);
      } else {
        // if empty, maybe go to inventory without search or do nothing?
        // Let's go to full inventory
        navigate('/inventory');
      }
    }
  };

  return (
    <input
      type="text"
      placeholder="Buscar herramientas..."
      value={term}
      onChange={(e) => setTerm(e.target.value)}
      onKeyDown={handleKeyDown}
      style={{
        padding: '8px 12px',
        borderRadius: '6px',
        border: '1px solid #ccc',
        width: '300px',
        backgroundColor: '#ffffff',
        color: '#000000',
      }}
    />
  );
};

export default SearchBar;
