import React, { createContext, useCallback, useState, useContext } from 'react';
import TransitionAlert from './TransitionAlert';

/**
 * Context for managing global alerts.
 * Provides methods to show and hide alerts.
 */
export const AlertContext = createContext({
  show: (a) => {},
  hide: () => {},
});

export const useAlert = () => useContext(AlertContext);

// Global function to show alerts from non-React code (like axios interceptors)
let globalShowAlert = null;

export const setGlobalShowAlert = (fn) => {
  globalShowAlert = fn;
};

export const showGlobalAlert = (alert) => {
  if (globalShowAlert) {
    globalShowAlert(alert);
  }
};

/**
 * Provider component for the AlertContext.
 * Manages the state of the current alert and renders the TransitionAlert component.
 */
export const AlertProvider = ({ children }) => {
  const [alert, setAlert] = useState(null);

  // Displays an alert with the specified properties.
  const show = useCallback((a) => {
    setAlert({ ...a });
  }, []);

  // Hides the current alert.
  const hide = useCallback(() => setAlert(null), []);

  // Register global alert function
  React.useEffect(() => {
    setGlobalShowAlert(show);
    return () => setGlobalShowAlert(null);
  }, [show]);

  return (
    <AlertContext.Provider value={{ show, hide }}>
      {children}
      <TransitionAlert
        alert={alert}
        onClose={() => setAlert(null)}
        autoHideMs={alert?.autoHideMs}
        offsetTop={alert?.offsetTop}
      />
    </AlertContext.Provider>
  );
};
