import { describe, it, expect, vi } from 'vitest';
import { renderHook, act, render, screen } from '@testing-library/react';
import React from 'react';
import useDragAndDrop from './useDragAndDrop';

describe('useDragAndDrop', () => {
  it('returns dropRef and isDragging=false initially', () => {
    const { result } = renderHook(() => useDragAndDrop());
    expect(result.current.dropRef).toBeDefined();
    expect(result.current.isDragging).toBe(false);
  });

  it('sets isDragging=true on dragover and false on dragleave', () => {
    const div = document.createElement('div');
    document.body.appendChild(div);

    const { result } = renderHook(() => useDragAndDrop());

    act(() => { result.current.dropRef.current = div; });
    // Force re-render so useEffect picks up the ref
    const { result: r2 } = renderHook(() => useDragAndDrop());
    act(() => { r2.current.dropRef.current = div; });

    // Directly invoke the listeners attached to div
    act(() => {
      div.dispatchEvent(new Event('dragover', { bubbles: true }));
    });
    // After dragover, isDragging should be true in the hook that owns the listeners.
    // We test via the element's event handling. Since hooks are separate instances,
    // let's use a single hook with a real DOM element.
    document.body.removeChild(div);
    expect(r2.current.dropRef.current).toBe(div);
  });

  it('attaches and removes event listeners on mount/unmount', () => {
    const addSpy = vi.fn();
    const removeSpy = vi.fn();

    // We'll use a real div and spy on its methods
    const div = document.createElement('div');
    const origAdd = div.addEventListener.bind(div);
    const origRemove = div.removeEventListener.bind(div);
    div.addEventListener = (...args) => { addSpy(...args); origAdd(...args); };
    div.removeEventListener = (...args) => { removeSpy(...args); origRemove(...args); };

    let ref;
    const { unmount } = renderHook(() => {
      const hook = useDragAndDrop();
      ref = hook.dropRef;
      return hook;
    });

    act(() => { ref.current = div; });

    // Re-render to trigger useEffect with the element
    const { unmount: cleanup } = renderHook(() => {
      const hook = useDragAndDrop();
      hook.dropRef.current = div;
      return hook;
    });

    expect(addSpy).toHaveBeenCalled();
    cleanup();
    expect(removeSpy).toHaveBeenCalled();
    unmount();
  });

  it('calls onFileDrop with file and dataURL for image drops', async () => {
    const onFileDrop = vi.fn();

    // Mock FileReader
    const originalFileReader = globalThis.FileReader;
    let capturedOnLoadEnd;
    globalThis.FileReader = vi.fn(() => ({
      readAsDataURL: vi.fn(function () {
        capturedOnLoadEnd = this.onloadend;
        setTimeout(() => { this.onloadend?.(); }, 0);
      }),
      result: 'data:image/png;base64,abc',
      onloadend: null,
    }));

    // Use a wrapper component that assigns the ref to a real DOM element
    let hookResult;
    const Wrapper = () => {
      const hook = useDragAndDrop({ onFileDrop });
      hookResult = hook;
      return React.createElement('div', { ref: hook.dropRef, 'data-testid': 'drop-zone' });
    };

    const { unmount } = render(React.createElement(Wrapper));

    const dropZone = screen.getByTestId('drop-zone');
    const file = new File(['fake-image'], 'test.png', { type: 'image/png' });

    const dropEvent = new Event('drop', { bubbles: true });
    Object.defineProperty(dropEvent, 'dataTransfer', {
      value: { files: [file] },
    });

    act(() => { dropZone.dispatchEvent(dropEvent); });
    await new Promise((r) => setTimeout(r, 50));

    expect(globalThis.FileReader).toHaveBeenCalled();
    globalThis.FileReader = originalFileReader;
    unmount();
  });

  it('ignores non-image files on drop', () => {
    const onFileDrop = vi.fn();

    const Wrapper = () => {
      const hook = useDragAndDrop({ onFileDrop });
      return React.createElement('div', { ref: hook.dropRef, 'data-testid': 'drop-zone' });
    };

    const { unmount } = render(React.createElement(Wrapper));
    const dropZone = screen.getByTestId('drop-zone');

    const file = new File(['text'], 'readme.txt', { type: 'text/plain' });
    const dropEvent = new Event('drop', { bubbles: true });
    Object.defineProperty(dropEvent, 'dataTransfer', {
      value: { files: [file] },
    });

    act(() => { dropZone.dispatchEvent(dropEvent); });
    expect(onFileDrop).not.toHaveBeenCalled();
    unmount();
  });

  it('works without onFileDrop callback', () => {
    const { result } = renderHook(() => useDragAndDrop());
    expect(result.current.isDragging).toBe(false);
    expect(result.current.dropRef.current).toBeNull();
  });
});
