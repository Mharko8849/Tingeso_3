import { describe, it, expect } from 'vitest';
import { statusToBadgeVariant } from './statusToBadge';

describe('statusToBadgeVariant', () => {
  it('maps ACTIVO to green', () => {
    expect(statusToBadgeVariant('ACTIVO')).toBe('green');
    expect(statusToBadgeVariant('activo')).toBe('green');
    expect(statusToBadgeVariant(' Activo ')).toBe('green');
  });

  it('maps PENDIENTE to yellow', () => {
    expect(statusToBadgeVariant('PENDIENTE')).toBe('yellow');
  });

  it('maps RESTRINGIDO to red', () => {
    expect(statusToBadgeVariant('RESTRINGIDO')).toBe('red');
  });

  it('maps FINALIZADO to blue', () => {
    expect(statusToBadgeVariant('FINALIZADO')).toBe('blue');
  });

  it('returns undefined for unknown status', () => {
    expect(statusToBadgeVariant('UNKNOWN')).toBeUndefined();
    expect(statusToBadgeVariant('')).toBeUndefined();
  });

  it('handles null/undefined gracefully', () => {
    expect(statusToBadgeVariant(null)).toBeUndefined();
    expect(statusToBadgeVariant(undefined)).toBeUndefined();
  });
});
