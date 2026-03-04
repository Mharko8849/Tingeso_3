import React, { useState } from 'react';
import PropTypes from 'prop-types';
import './Tooltip.css';

const GAP = 8;

const calcPositionStyle = (pos, rect, vw, vh) => {
  const cx = `${rect.left + rect.width / 2}px`;
  const cy = `${rect.top + rect.height / 2}px`;
  const topAbove = `${vh - rect.top + GAP}px`;
  const topBelow = `${rect.bottom + GAP}px`;
  const leftRight = `${rect.right + GAP}px`;
  const rightLeft = `${vw - rect.left + GAP}px`;
  return {
    right: { top: cy, left: leftRight, transform: 'translateY(-50%)' },
    left: { top: cy, right: rightLeft, transform: 'translateY(-50%)' },
    bottom: { top: topBelow, left: cx, transform: 'translateX(-50%)' },
    top: { bottom: topAbove, left: cx, transform: 'translateX(-50%)' },
  }[pos];
};

const shouldFlip = (position, rect, viewportWidth, viewportHeight) => {
  if (position === 'right') return rect.right > viewportWidth - 250;
  if (position === 'left') return rect.left < 250;
  if (position === 'bottom') return rect.bottom > viewportHeight - 100;
  if (position === 'top') return rect.top < 100;
  return false;
};

const OPPOSITE = { right: 'left', left: 'right', bottom: 'top', top: 'bottom' };

const calcTooltipPosition = (position, rect, viewportWidth, viewportHeight) => {
  const flip = shouldFlip(position, rect, viewportWidth, viewportHeight);
  const newPosition = flip ? OPPOSITE[position] : position;
  const style = calcPositionStyle(newPosition, rect, viewportWidth, viewportHeight);
  return { newPosition, style };
};

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
const Tooltip = ({ text, children, position = 'top', maxWidth = '250px' }) => (
    <div className="tooltip-wrapper">
      {children}
      {text && (
        <div className={`tooltip-content tooltip-${position}`} style={{ maxWidth }}>
          {text}
          <div className={`tooltip-arrow tooltip-arrow-${position}`} />
        </div>
      )}
    </div>
  );

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
    if (!iconRef.current) return;
    const rect = iconRef.current.getBoundingClientRect();
    const { newPosition, style } = calcTooltipPosition(position, rect, globalThis.innerWidth, globalThis.innerHeight);
    setActualPosition(newPosition);
    setTooltipStyle(style);
  };

  return (
    <button 
      type="button"
      ref={iconRef}
      className="help-icon"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={() => setVisible(false)}
      aria-label="Ayuda"
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setVisible((v) => !v); }}
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
    </button>
  );
};

HelpIcon.propTypes = {
  content: PropTypes.string,
  position: PropTypes.oneOf(['top', 'bottom', 'left', 'right']),
};

Tooltip.propTypes = {
  text: PropTypes.string,
  children: PropTypes.node,
  position: PropTypes.oneOf(['top', 'bottom', 'left', 'right']),
  maxWidth: PropTypes.string,
};

export default Tooltip;
