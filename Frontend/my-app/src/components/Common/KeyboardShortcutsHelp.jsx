import React, { useState } from 'react';
import { HelpIcon } from './Tooltip';
import { useEscapeKey } from '../../hooks/useKeyboardShortcuts';
import '../Tools/ToolDropdown.css';

/**
 * KeyboardShortcutsHelp component
 * Displays a modal with available keyboard shortcuts.
 * Follows Nielsen's Heuristic #10: Help and Documentation
 */
const KeyboardShortcutsHelp = () => {
  const [isOpen, setIsOpen] = useState(false);

  // Close modal with Escape key (practicing what we preach!)
  useEscapeKey(() => setIsOpen(false), isOpen);

  const shortcuts = [
    { key: 'Escape', description: 'Cerrar modal o diálogo abierto' },
    { key: 'Ctrl + Enter', description: 'Enviar formulario activo' },
    { key: 'Enter', description: 'Guardar cambios en edición' },
    { key: 'Enter', description: 'Buscar en barra de búsqueda' },
  ];

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="link"
        style={{ 
          display: 'inline-flex', 
          alignItems: 'center', 
          gap: '6px', 
          fontSize: '0.875rem',
          padding: '4px 8px',
          cursor: 'pointer'
        }}
        aria-label="Atajos de teclado"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ color: '#2b6cb0' }}>
          <rect x="2" y="5" width="20" height="14" rx="2" stroke="currentColor" strokeWidth="2"/>
          <rect x="5" y="8" width="2" height="2" fill="currentColor"/>
          <rect x="9" y="8" width="2" height="2" fill="currentColor"/>
          <rect x="13" y="8" width="2" height="2" fill="currentColor"/>
          <rect x="17" y="8" width="2" height="2" fill="currentColor"/>
          <rect x="5" y="12" width="2" height="2" fill="currentColor"/>
          <rect x="9" y="12" width="2" height="2" fill="currentColor"/>
          <rect x="13" y="12" width="2" height="2" fill="currentColor"/>
          <rect x="17" y="12" width="2" height="2" fill="currentColor"/>
          <rect x="7" y="16" width="10" height="2" rx="1" fill="currentColor"/>
        </svg>
      </button>

      {isOpen && (
        <div className="tool-overlay" onClick={() => setIsOpen(false)}>
          <div 
            className="tool-content" 
            onClick={(e) => e.stopPropagation()} 
            style={{ maxWidth: '500px', width: '90%' }}
          >
            <button className="close-btn" onClick={() => setIsOpen(false)} aria-label="Cerrar">
              ✕
            </button>
            
            <h3 style={{ marginTop: 0, display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              Atajos de Teclado
              <HelpIcon 
                content="Navega más rápido sin usar el mouse."
                position="right"
              />
            </h3>
            
            <p style={{ color: '#4b5563', marginBottom: '20px' }}>
              Acelera tu trabajo con estos atajos de teclado disponibles en toda la aplicación.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {shortcuts.map((shortcut, idx) => (
                <div 
                  key={idx}
                  style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    padding: '12px',
                    background: '#f9fafb',
                    borderRadius: '6px'
                  }}
                >
                  <span style={{ fontSize: '0.875rem', color: '#374151' }}>
                    {shortcut.description}
                  </span>
                  <kbd 
                    style={{ 
                      padding: '4px 8px', 
                      background: '#fff', 
                      border: '1px solid #d1d5db',
                      borderRadius: '4px',
                      fontSize: '0.8125rem',
                      fontFamily: 'monospace',
                      fontWeight: '600',
                      color: '#1a202c',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                    }}
                  >
                    {shortcut.key}
                  </kbd>
                </div>
              ))}
            </div>

            <div 
              style={{ 
                marginTop: '20px', 
                padding: '12px', 
                background: '#eff6ff', 
                borderLeft: '4px solid #60a5fa',
                borderRadius: '4px'
              }}
            >
              <p style={{ margin: 0, fontSize: '0.875rem', color: '#1e40af' }}>
                <strong>Tip:</strong> Los atajos funcionan en contextos relevantes. Por ejemplo, 
                Ctrl+Enter enviará el formulario solo cuando estés dentro de un formulario.
              </p>
            </div>

            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
              <button 
                className="primary-cta" 
                onClick={() => setIsOpen(false)}
                style={{ minWidth: '120px', justifyContent: 'center' }}
              >
                Entendido
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default KeyboardShortcutsHelp;
