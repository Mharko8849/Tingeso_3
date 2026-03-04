import { describe, it, expect, vi } from 'vitest';
import { handleNumericInput } from './numericInput';

describe('handleNumericInput', () => {
  it('accepts empty string', () => {
    const setForm = vi.fn();
    handleNumericInput('repoCost', '', setForm, null);
    expect(setForm).toHaveBeenCalledTimes(1);
    // Verify the updater function works correctly
    const updater = setForm.mock.calls[0][0];
    expect(updater({ repoCost: '100', other: 'x' })).toEqual({ repoCost: '', other: 'x' });
  });

  it('accepts positive integer strings', () => {
    const setForm = vi.fn();
    handleNumericInput('priceRent', '42', setForm, null);
    expect(setForm).toHaveBeenCalledTimes(1);
    const updater = setForm.mock.calls[0][0];
    expect(updater({ priceRent: '' })).toEqual({ priceRent: '42' });
  });

  it('accepts zero', () => {
    const setForm = vi.fn();
    handleNumericInput('repoCost', '0', setForm, null);
    expect(setForm).toHaveBeenCalledTimes(1);
  });

  it('rejects negative numbers and shows alert', () => {
    const setForm = vi.fn();
    const alert = { show: vi.fn() };
    handleNumericInput('repoCost', '-5', setForm, alert);
    expect(setForm).not.toHaveBeenCalled();
    expect(alert.show).toHaveBeenCalledWith({
      severity: 'warning',
      message: 'Debe ingresar valores enteros positivos',
      autoHideMs: 3500,
    });
  });

  it('rejects decimal numbers', () => {
    const setForm = vi.fn();
    const alert = { show: vi.fn() };
    handleNumericInput('repoCost', '3.5', setForm, alert);
    expect(setForm).not.toHaveBeenCalled();
    expect(alert.show).toHaveBeenCalled();
  });

  it('rejects non-numeric strings', () => {
    const setForm = vi.fn();
    const alert = { show: vi.fn() };
    handleNumericInput('repoCost', 'abc', setForm, alert);
    expect(setForm).not.toHaveBeenCalled();
    expect(alert.show).toHaveBeenCalled();
  });

  it('handles null alert gracefully', () => {
    const setForm = vi.fn();
    expect(() => handleNumericInput('repoCost', 'abc', setForm, null)).not.toThrow();
    expect(setForm).not.toHaveBeenCalled();
  });

  it('handles alert without show method gracefully', () => {
    const setForm = vi.fn();
    expect(() => handleNumericInput('repoCost', 'abc', setForm, {})).not.toThrow();
  });
});
