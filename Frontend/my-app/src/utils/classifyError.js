/**
 * Classifies an error into a user-friendly message.
 * Shared between stock/tool modal components.
 * @param {Error|object} e - The error object
 * @param {string} [defaultMessage] - Fallback message
 * @returns {string} User-friendly error message
 */
export const classifyError = (e, defaultMessage = 'Ocurrió un error desconocido. Contacte al administrador.') => {
  if (!e) return defaultMessage;
  if (e.code === 'ERR_NETWORK' || e.message === 'Network Error' || !e.response) {
    return 'Error de conexión. Verifique su conexión a internet e intente nuevamente.';
  }
  const status = e.response?.status;
  if (status === 401 || status === 403) {
    return 'No tiene permisos para realizar esta acción. Inicie sesión nuevamente.';
  }
  if (status === 400) {
    const data = e.response?.data;
    return typeof data === 'string' ? data : 'Datos inválidos. Revise los campos e intente nuevamente.';
  }
  if (status === 409) {
    return 'Ya existe un registro con ese nombre. Use un nombre diferente.';
  }
  if (status >= 500) {
    return 'Error interno del servidor. Contacte al administrador del sistema.';
  }
  if (e.message) return e.message;
  return defaultMessage;
};
