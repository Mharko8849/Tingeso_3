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

  return (
    <span 
      className="help-icon"
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      aria-label="Ayuda"
    >
      ?
      {visible && content && (
        <span className={`tooltip-content tooltip-${position}`} style={{ maxWidth: '280px' }}>
          {content}
          <span className={`tooltip-arrow tooltip-arrow-${position}`} />
        </span>
      )}
    </span>
  );
};

export default Tooltip;
