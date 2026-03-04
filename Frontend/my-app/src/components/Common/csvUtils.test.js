import { describe, it, expect, vi, beforeEach } from 'vitest';
import { quoteCell, buildCsv, downloadBlob } from './csvUtils';

describe('quoteCell', () => {
  it('wraps string value in quotes', () => {
    expect(quoteCell('hello')).toBe('"hello"');
  });

  it('escapes internal double quotes', () => {
    expect(quoteCell('say "hi"')).toBe('"say ""hi"""');
  });

  it('handles null/undefined as empty string', () => {
    expect(quoteCell(null)).toBe('""');
    expect(quoteCell(undefined)).toBe('""');
  });

  it('converts numbers to string', () => {
    expect(quoteCell(42)).toBe('"42"');
  });
});

describe('buildCsv', () => {
  it('builds CSV from headers and rows', () => {
    const csv = buildCsv(['Name', 'Age'], [['Alice', '30'], ['Bob', '25']]);
    expect(csv).toContain('"Name","Age"');
    expect(csv).toContain('"Alice","30"');
    expect(csv).toContain('"Bob","25"');
  });

  it('includes BOM by default', () => {
    const csv = buildCsv(['H'], [['V']]);
    expect(csv.codePointAt(0)).toBe(0xFEFF);
  });

  it('omits BOM when useBom is false', () => {
    const csv = buildCsv(['H'], [['V']], ',', false);
    expect(csv.codePointAt(0)).not.toBe(0xFEFF);
  });

  it('uses custom delimiter', () => {
    const csv = buildCsv(['A', 'B'], [['1', '2']], ';');
    expect(csv).toContain('"A";"B"');
  });
});

describe('downloadBlob', () => {
  beforeEach(() => {
    // Mock DOM methods
    const mockLink = {
      href: '',
      download: '',
      click: vi.fn(),
      remove: vi.fn(),
    };
    vi.spyOn(document, 'createElement').mockReturnValue(mockLink);
    vi.spyOn(document.body, 'appendChild').mockReturnValue(undefined);
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test');
    vi.spyOn(URL, 'revokeObjectURL').mockReturnValue(undefined);
  });

  it('creates and clicks a download link', () => {
    downloadBlob('content', 'test.csv');
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
  });
});
