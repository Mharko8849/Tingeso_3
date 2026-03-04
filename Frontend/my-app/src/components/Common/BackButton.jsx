import React from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';

/**
 * A simple link-style "Volver" (Back) button used across admin pages.
 * Input: props (label, to, onClick, className, style)
 * Output: JSX Element (button)
 */
const BackButton = ({ label = 'Volver', to = '...', onClick, className = '', style = {} }) => {
  const navigate = useNavigate();
  
  /**
   * Handles the click event.
   * If an onClick prop is provided, it calls it.
   * Otherwise, it navigates to the target path using React Router.
   * Input: click event
   * Output: void
   */
  const handle = (e) => {
    if (onClick) return onClick(e);
    // Use React Router navigation
    try {
      navigate(to);
    } catch (error_) { console.debug(error_);
      globalThis.location.href = to;
    }
  };

  // ensure consistent visual size with other link buttons
  const mergedStyle = { fontSize: 18, lineHeight: '20px', padding: 0, ...style };

  return (
    <button type="button" className={`link ${className}`.trim()} onClick={handle} style={mergedStyle}>{label}</button>
  );
};

BackButton.propTypes = {
  className: PropTypes.string,
  label: PropTypes.string,
  onClick: PropTypes.func,
  style: PropTypes.object,
  to: PropTypes.string,
};

export default BackButton;
