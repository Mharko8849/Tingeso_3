import { useEffect, useCallback } from 'react';

/**
 * Custom hook for handling keyboard shortcuts.
 * Follows Nielsen's Heuristic #7: Flexibility and Efficiency of Use
 * 
 * @param {Object} shortcuts - Object mapping key combinations to handlers
 * @example
 * useKeyboardShortcuts({
 *   'Escape': () => closeModal(),
 *   'Control+Enter': () => submitForm(),
 *   'Control+KeyS': (e) => { e.preventDefault(); save(); }
 * });
 */
export const useKeyboardShortcuts = (shortcuts) => {
  const handleKeyDown = useCallback(
    (event) => {
      // Build key combination string
      const keys = [];
      if (event.ctrlKey || event.metaKey) keys.push('Control');
      if (event.altKey) keys.push('Alt');
      if (event.shiftKey) keys.push('Shift');
      keys.push(event.key);
      
      const combination = keys.join('+');

      // Check for matching shortcut
      if (shortcuts[combination]) {
        shortcuts[combination](event);
      } else if (shortcuts[event.key]) {
        shortcuts[event.key](event);
      }
    },
    [shortcuts]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);
};

/**
 * Hook for implementing Escape key to close modals/overlays.
 * 
 * @param {Function} onClose - Callback function to execute on Escape
 * @param {boolean} [enabled=true] - Whether the hook is enabled
 */
export const useEscapeKey = (onClose, enabled = true) => {
  useEffect(() => {
    if (!enabled) return;

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onClose, enabled]);
};

/**
 * Hook for implementing Ctrl+Enter to submit forms.
 * 
 * @param {Function} onSubmit - Callback function to execute on Ctrl+Enter
 * @param {boolean} [enabled=true] - Whether the hook is enabled
 */
export const useCtrlEnter = (onSubmit, enabled = true) => {
  useEffect(() => {
    if (!enabled) return;

    const handleCtrlEnter = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        onSubmit();
      }
    };

    document.addEventListener('keydown', handleCtrlEnter);
    return () => {
      document.removeEventListener('keydown', handleCtrlEnter);
    };
  }, [onSubmit, enabled]);
};

/**
 * Hook for implementing Enter key on specific elements.
 * Useful for search bars and inline inputs.
 * 
 * @param {Function} onEnter - Callback function to execute on Enter
 * @param {React.RefObject} ref - Ref to the target element
 */
export const useEnterKey = (onEnter, ref) => {
  useEffect(() => {
    const element = ref?.current;
    if (!element) return;

    const handleEnter = (event) => {
      if (event.key === 'Enter') {
        event.preventDefault();
        onEnter();
      }
    };

    element.addEventListener('keydown', handleEnter);
    return () => {
      element.removeEventListener('keydown', handleEnter);
    };
  }, [onEnter, ref]);
};

export default useKeyboardShortcuts;
