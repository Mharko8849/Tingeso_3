import { describe, it, expect } from 'vitest';
import { validateEmail, validatePassword, validateUsername, validateField, formatDate, formatDateTime } from './validation';

describe('validateEmail', () => {
  it('rejects null/undefined/non-string', () => {
    expect(validateEmail(null).isValid).toBe(false);
    expect(validateEmail(undefined).isValid).toBe(false);
    expect(validateEmail(123).isValid).toBe(false);
  });

  it('rejects empty string', () => {
    expect(validateEmail('').isValid).toBe(false);
    expect(validateEmail('   ').isValid).toBe(false);
  });

  it('rejects invalid format', () => {
    expect(validateEmail('notanemail').isValid).toBe(false);
    expect(validateEmail('no@').isValid).toBe(false);
  });

  it('accepts valid email', () => {
    expect(validateEmail('user@example.com').isValid).toBe(true);
  });

  it('rejects email longer than 254 chars', () => {
    const long = `${'a'.repeat(250)  }@b.com`;
    expect(validateEmail(long).isValid).toBe(false);
  });

  it('rejects local part longer than 64 chars', () => {
    const long = `${'a'.repeat(65)  }@example.com`;
    expect(validateEmail(long).isValid).toBe(false);
  });

  it('rejects domain shorter than 4 chars', () => {
    expect(validateEmail('a@b.c').isValid).toBe(false);
  });
});

describe('validatePassword', () => {
  it('rejects empty/null', () => {
    expect(validatePassword(null).isValid).toBe(false);
    expect(validatePassword('').isValid).toBe(false);
  });

  it('rejects too short (< 6)', () => {
    expect(validatePassword('ab').isValid).toBe(false);
    expect(validatePassword('ab').strength).toBe('weak');
  });

  it('accepts fair password (6-7 chars)', () => {
    const result = validatePassword('abcdef');
    expect(result.isValid).toBe(true);
    expect(result.strength).toBe('fair');
  });

  it('returns strong for 8+ with mixed case and numbers', () => {
    const result = validatePassword('Abcdef1X');
    expect(result.isValid).toBe(true);
    expect(result.strength).toBe('strong');
  });

  it('returns fair for 8+ without mixed case', () => {
    const result = validatePassword('abcdefgh');
    expect(result.isValid).toBe(true);
    expect(result.strength).toBe('fair');
  });
});

describe('validateUsername', () => {
  it('rejects null/undefined/non-string', () => {
    expect(validateUsername(null).isValid).toBe(false);
    expect(validateUsername(undefined).isValid).toBe(false);
  });

  it('rejects too short (< 3 chars)', () => {
    expect(validateUsername('ab').isValid).toBe(false);
  });

  it('rejects too long (> 30 chars)', () => {
    expect(validateUsername('a'.repeat(31)).isValid).toBe(false);
  });

  it('rejects special characters', () => {
    expect(validateUsername('user@name').isValid).toBe(false);
    expect(validateUsername('user name').isValid).toBe(false);
  });

  it('accepts valid username with underscores and hyphens', () => {
    expect(validateUsername('my_user-1').isValid).toBe(true);
  });
});

describe('validateField', () => {
  it('validates email type', () => {
    expect(validateField('email', 'a@b.com').isValid).toBe(true);
  });

  it('validates password type', () => {
    expect(validateField('password', 'Abc12345').isValid).toBe(true);
  });

  it('validates username type', () => {
    expect(validateField('username', 'myuser').isValid).toBe(true);
  });

  it('validates rut type', () => {
    // Valid Chilean RUT
    const result = validateField('rut', '11.111.111-1');
    expect(result).toHaveProperty('isValid');
  });

  it('returns valid for unknown field type', () => {
    expect(validateField('unknown', 'anything').isValid).toBe(true);
  });
});

describe('formatDate', () => {
  it('formats YYYY-MM-DD to DD/MM/YYYY', () => {
    expect(formatDate('2025-01-15')).toBe('15/01/2025');
  });

  it('returns empty string for falsy input', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('')).toBe('');
  });

  it('returns original string for invalid date', () => {
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });
});

describe('formatDateTime', () => {
  it('formats datetime string to DD/MM/YYYY HH:mm', () => {
    const result = formatDateTime('2025-06-15T14:30:00');
    expect(result).toMatch(/15\/06\/2025/);
  });

  it('returns empty string for falsy input', () => {
    expect(formatDateTime(null)).toBe('');
    expect(formatDateTime('')).toBe('');
  });

  it('returns original for invalid date', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });
});
