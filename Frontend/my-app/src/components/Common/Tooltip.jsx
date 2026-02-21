import React, { useState } from 'react';
import './Tooltip.css';

/**
 * Tooltip component for providing contextual help information.
 * Follows Nielsen's Heuristic #10: Help and Documentation
 * 
 * @param {Object} props
 * @param {string} props.text - The tooltip text to display
 * @param {React.ReactNode} props.children - The element that triggers the tooltip
 * @param {string} [props.position='top'] - Position of tooltip: 'top', 'bottom', 'left', 'right'
 * @param {string} [props.maxWidth='250px'] - Maximum width of tooltip
 * @returns {JSX.Element}
 */
const Tooltip = ({ text, children, position = 'top', maxWidth = '250px' }) => {
  const [visible, setVisible] = useState(false);

  const handleMouseEnter = () => setVisible(true);
  const handleMouseLeave = () => setVisible(false);

  return (
    <div className="tooltip-wrapper" onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
      {children}
      {visible && text && (
        <div className={`tooltip-content tooltip-${position}`} style={{ maxWidth }}>
          {text}
          <div className={`tooltip-arrow tooltip-arrow-${position}`} />
        </div>
      )}
    </div>
  );
};

/**
 * HelpIcon component - A small question mark icon with tooltip
 * Perfect for inline help next to labels
 * 
 * @param {Object} props
 * @param {string} props.content - The help text to display
 * @param {string} [props.position='right'] - Tooltip position
 * @returns {JSX.Element}
 */
export const HelpIcon = ({ content, position = 'right' }) => {
  const [visible, setVisible] = useState(false);
  const [actualPosition, setActualPosition] = useState(position);
  const [tooltipStyle, setTooltipStyle] = useState({});
  const iconRef = React.useRef(null);

  const handleMouseEnter = () => {
    setVisible(true);
    
    // Smart positioning: adjust based on viewport space
    if (iconRef.current) {
      const rect = iconRef.current.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      
      let newPosition = position;
      let style = {};
      
      // Calculate fixed position based on icon location
      const gap = 8;
      
      // Check if there's space on the right
      if (position === 'right' && rect.right > viewportWidth - 250) {
        newPosition = 'left';
        style = {
          top: `${rect.top + rect.height / 2}px`,
          right: `${viewportWidth - rect.left + gap}px`,
          transform: 'translateY(-50%)'
        };
      } else if (position === 'right') {
        style = {
          top: `${rect.top + rect.height / 2}px`,
          left: `${rect.right + gap}px`,
          transform: 'translateY(-50%)'
        };
      }
      
      // Check if there's space on the left
      if (position === 'left' && rect.left < 250) {
        newPosition = 'right';
        style = {
          top: `${rect.top + rect.height / 2}px`,
          left: `${rect.right + gap}px`,
          transform: 'translateY(-50%)'
        };
      } else if (position === 'left') {
        style = {
          top: `${rect.top + rect.height / 2}px`,
          right: `${viewportWidth - rect.left + gap}px`,
          transform: 'translateY(-50%)'
        };
      }
      
      // Check if there's space on bottom
      if (position === 'bottom' && rect.bottom > viewportHeight - 100) {
        newPosition = 'top';
        style = {
          bottom: `${viewportHeight - rect.top + gap}px`,
          left: `${rect.left + rect.width / 2}px`,
          transform: 'translateX(-50%)'
        };
      } else if (position === 'bottom') {
        style = {
          top: `${rect.bottom + gap}px`,
          left: `${rect.left + rect.width / 2}px`,
          transform: 'translateX(-50%)'
        };
      }
      
      // Check if there's space on top
      if (position === 'top' && rect.top < 100) {
        newPosition = 'bottom';
        style = {
          top: `${rect.bottom + gap}px`,
          left: `${rect.left + rect.width / 2}px`,
          transform: 'translateX(-50%)'
        };
      } else if (position === 'top') {
        style = {
          bottom: `${viewportHeight - rect.top + gap}px`,
          left: `${rect.left + rect.width / 2}px`,
          transform: 'translateX(-50%)'
        };
      }
      
      setActualPosition(newPosition);
      setTooltipStyle(style);
    }
  };

  return (
    <span 
      ref={iconRef}
      className="help-icon"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={() => setVisible(false)}
      aria-label="Ayuda"
    >
      ?
      {visible && content && (
        <span 
          className={`tooltip-content tooltip-fixed tooltip-${actualPosition}`}
          style={tooltipStyle}
        >
          {content}
          <span className={`tooltip-arrow tooltip-arrow-${actualPosition}`} />
        </span>
      )}
    </span>
  );
};

export default Tooltip;
