import React from 'react';
import PropTypes from 'prop-types';
import ToolCard from './ToolCard';

// ToolGrid now accepts `toolCardProps` which are forwarded to each ToolCard.
const ToolGrid = ({ tools = [], toolCardProps = {} }) => {
  if (!tools || tools.length === 0) return <div>No se encontraron herramientas.</div>;

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: 18 }}>
      {tools.map((t) => {
        // allow addDisabled to be computed per-tool when a function is provided
        const resolvedAddDisabled = typeof toolCardProps.addDisabled === 'function' ? toolCardProps.addDisabled(t) : toolCardProps.addDisabled;
        const perToolProps = { ...toolCardProps, addDisabled: resolvedAddDisabled };
        return <ToolCard key={t.id} tool={t} style={{ margin: 0 }} {...perToolProps} />;
      })}
    </div>
  );
};

ToolGrid.propTypes = {
  toolCardProps: PropTypes.object,
  tools: PropTypes.array,
};

export default ToolGrid;
