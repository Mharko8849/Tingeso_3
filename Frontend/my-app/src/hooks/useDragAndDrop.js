import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Custom hook for drag-and-drop file upload functionality.
 * @param {object} options
 * @param {function} options.onFileDrop - Callback when a valid image file is dropped (receives File, dataURL)
 * @returns {{ dropRef, isDragging }}
 */
const useDragAndDrop = ({ onFileDrop } = {}) => {
  const dropRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDropEvent = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files?.[0];
    if (droppedFile?.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        onFileDrop?.(droppedFile, reader.result);
      };
      reader.readAsDataURL(droppedFile);
    }
  }, [onFileDrop]);

  useEffect(() => {
    const el = dropRef.current;
    if (!el) return;
    el.addEventListener('dragover', handleDragOver);
    el.addEventListener('dragleave', handleDragLeave);
    el.addEventListener('drop', handleDropEvent);
    return () => {
      el.removeEventListener('dragover', handleDragOver);
      el.removeEventListener('dragleave', handleDragLeave);
      el.removeEventListener('drop', handleDropEvent);
    };
  }, [handleDragOver, handleDragLeave, handleDropEvent]);

  return { dropRef, isDragging };
};

export default useDragAndDrop;
