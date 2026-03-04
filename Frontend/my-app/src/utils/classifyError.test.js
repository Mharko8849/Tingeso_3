import { describe, it, expect } from 'vitest';
import { classifyError } from './classifyError';

describe('classifyError', () => {
  it('returns default message for null/undefined error', () => {
    expect(classifyError(null)).toBe('Ocurrió un error desconocido. Contacte al administrador.');
    expect(classifyError(undefined)).toBe('Ocurrió un error desconocido. Contacte al administrador.');
  });

  it('returns custom default message when provided', () => {
    expect(classifyError(null, 'Custom fallback')).toBe('Custom fallback');
  });

  it('returns network error message for ERR_NETWORK code', () => {
    const err = { code: 'ERR_NETWORK', message: 'Network Error' };
    expect(classifyError(err)).toBe('Error de conexión. Verifique su conexión a internet e intente nuevamente.');
  });

  it('returns network error message for "Network Error" message', () => {
    const err = { message: 'Network Error' };
    expect(classifyError(err)).toBe('Error de conexión. Verifique su conexión a internet e intente nuevamente.');
  });

  it('returns network error when no response object', () => {
    const err = { message: 'Something happened' };
    expect(classifyError(err)).toBe('Error de conexión. Verifique su conexión a internet e intente nuevamente.');
  });

  it('returns permissions message for 401', () => {
    const err = { response: { status: 401 } };
    expect(classifyError(err)).toBe('No tiene permisos para realizar esta acción. Inicie sesión nuevamente.');
  });

  it('returns permissions message for 403', () => {
    const err = { response: { status: 403 } };
    expect(classifyError(err)).toBe('No tiene permisos para realizar esta acción. Inicie sesión nuevamente.');
  });

  it('returns string data for 400 with string response', () => {
    const err = { response: { status: 400, data: 'Campo inválido' } };
    expect(classifyError(err)).toBe('Campo inválido');
  });

  it('returns generic invalid data message for 400 with non-string data', () => {
    const err = { response: { status: 400, data: { field: 'error' } } };
    expect(classifyError(err)).toBe('Datos inválidos. Revise los campos e intente nuevamente.');
  });

  it('returns conflict message for 409', () => {
    const err = { response: { status: 409 } };
    expect(classifyError(err)).toBe('Ya existe un registro con ese nombre. Use un nombre diferente.');
  });

  it('returns server error for 500+', () => {
    const err = { response: { status: 500 } };
    expect(classifyError(err)).toBe('Error interno del servidor. Contacte al administrador del sistema.');
    expect(classifyError({ response: { status: 503 } })).toBe('Error interno del servidor. Contacte al administrador del sistema.');
  });

  it('returns error message for other errors with message', () => {
    const err = { response: { status: 422 }, message: 'Validation failed' };
    expect(classifyError(err)).toBe('Validation failed');
  });

  it('returns default message for unknown errors without message', () => {
    const err = { response: { status: 422 } };
    expect(classifyError(err)).toBe('Ocurrió un error desconocido. Contacte al administrador.');
  });
});
