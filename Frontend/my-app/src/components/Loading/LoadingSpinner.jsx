import React from 'react';
import PropTypes from 'prop-types';
import './LoadingSpinner.css';

/**
 * LoadingSpinner component
 * A reusable loading spinner that follows the app's design system.
 * Can be used for page transitions, data fetching, or any async operations.
 * 
 * @param {Object} props
 * @param {string} [props.size='medium'] - Size of spinner: 'small', 'medium', 'large'
 * @param {string} [props.message='Cargando...'] - Loading message to display
 * @param {boolean} [props.fullScreen=false] - If true, takes full screen with overlay
 * @param {string} [props.className=''] - Additional CSS classes
 * @returns {JSX.Element}
 */
const LoadingSpinner = ({ 
  size = 'medium', 
  message = 'Cargando...', 
  fullScreen = false,
  className = '',
}) => {
  const spinnerClasses = `loading-spinner loading-spinner-${size} ${className}`.trim();
  
  if (fullScreen) {
    return (
      <div className="loading-overlay">
        <div className="loading-content">
          <div className={spinnerClasses}>
            <div className="spinner-ring" />
            <div className="spinner-ring" />
            <div className="spinner-ring" />
            <div className="spinner-ring" />
          </div>
          {message && <p className="loading-message">{message}</p>}
        </div>
      </div>
    );
  }

  return (
    <div className="loading-inline">
      <div className={spinnerClasses}>
        <div className="spinner-ring" />
        <div className="spinner-ring" />
        <div className="spinner-ring" />
        <div className="spinner-ring" />
      </div>
      {message && <p className="loading-message">{message}</p>}
    </div>
  );
};

LoadingSpinner.propTypes = {
  className: PropTypes.string,
  fullScreen: PropTypes.bool,
  message: PropTypes.string,
  size: PropTypes.number,
};

export default LoadingSpinner;
