/**
 * Validation utilities for forms.
 * Follows Nielsen's Heuristics #5 (Error Prevention) and #9 (Error Recovery)
 */

/**
 * Validates Chilean RUT (Rol Único Tributario).
 * Format: XX.XXX.XXX-X or XXXXXXXX-X
 * 
 * @param {string} rut - RUT string to validate
 * @returns {Object} - { isValid: boolean, message: string, formatted: string }
 */
export const validateRUT = (rut) => {
  if (!rut || typeof rut !== 'string') {
    return { isValid: false, message: 'RUT es requerido', formatted: '' };
  }

  // Remove dots and hyphens, convert to uppercase
  const cleanRUT = rut.replace(/\./g, '').replace(/-/g, '').toUpperCase().trim();

  if (cleanRUT.length < 2) {
    return { isValid: false, message: 'RUT incompleto', formatted: rut };
  }

  // Separate body and verifier digit
  const body = cleanRUT.slice(0, -1);
  const verifier = cleanRUT.slice(-1);

  // Validate that body contains only numbers
  if (!/^\d+$/.test(body)) {
    return { isValid: false, message: 'RUT debe contener solo números', formatted: rut };
  }

  // Calculate verification digit
  let sum = 0;
  let multiplier = 2;

  for (let i = body.length - 1; i >= 0; i--) {
    sum += parseInt(body[i]) * multiplier;
    multiplier = multiplier === 7 ? 2 : multiplier + 1;
  }

  const calculatedVerifier = 11 - (sum % 11);
  let expectedVerifier;

  if (calculatedVerifier === 11) {
    expectedVerifier = '0';
  } else if (calculatedVerifier === 10) {
    expectedVerifier = 'K';
  } else {
    expectedVerifier = calculatedVerifier.toString();
  }

  if (verifier !== expectedVerifier) {
    return { 
      isValid: false, 
      message: `RUT inválido. Dígito verificador esperado: ${expectedVerifier}`,
      formatted: formatRUT(body + verifier)
    };
  }

  return { 
    isValid: true, 
    message: 'RUT válido',
    formatted: formatRUT(body + verifier)
  };
};

/**
 * Formats RUT to standard Chilean format: XX.XXX.XXX-X
 * 
 * @param {string} rut - RUT string to format
 * @returns {string} - Formatted RUT
 */
export const formatRUT = (rut) => {
  if (!rut) return '';

  const cleanRUT = rut.replace(/\./g, '').replace(/-/g, '').toUpperCase();
  
  if (cleanRUT.length < 2) return cleanRUT;

  const body = cleanRUT.slice(0, -1);
  const verifier = cleanRUT.slice(-1);

  // Add thousand separators
  const formatted = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  return `${formatted}-${verifier}`;
};

/**
 * Validates email address format.
 * 
 * @param {string} email - Email to validate
 * @returns {Object} - { isValid: boolean, message: string }
 */
export const validateEmail = (email) => {
  if (!email || typeof email !== 'string') {
    return { isValid: false, message: 'Email es requerido' };
  }

  const trimmedEmail = email.trim();

  if (trimmedEmail.length === 0) {
    return { isValid: false, message: 'Email es requerido' };
  }

  // Basic email regex pattern
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailPattern.test(trimmedEmail)) {
    return { 
      isValid: false, 
      message: 'Formato de email inválido. Ejemplo: usuario@ejemplo.com' 
    };
  }

  // Additional checks
  if (trimmedEmail.length > 254) {
    return { isValid: false, message: 'Email demasiado largo (máximo 254 caracteres)' };
  }

  const [localPart, domain] = trimmedEmail.split('@');

  if (localPart.length > 64) {
    return { isValid: false, message: 'Parte local del email demasiado larga' };
  }

  if (domain.length < 4) {
    return { isValid: false, message: 'Dominio del email muy corto' };
  }

  return { isValid: true, message: 'Email válido' };
};

/**
 * Validates password strength.
 * 
 * @param {string} password - Password to validate
 * @returns {Object} - { isValid: boolean, message: string, strength: string }
 */
export const validatePassword = (password) => {
  if (!password) {
    return { isValid: false, message: 'Contraseña es requerida', strength: 'none' };
  }

  if (password.length < 6) {
    return { 
      isValid: false, 
      message: 'Contraseña debe tener al menos 6 caracteres',
      strength: 'weak'
    };
  }

  if (password.length < 8) {
    return { 
      isValid: true, 
      message: 'Contraseña aceptable. Recomendado: al menos 8 caracteres',
      strength: 'fair'
    };
  }

  // Check for strong password (8+ chars with mixed case and numbers)
  const hasUpperCase = /[A-Z]/.test(password);
  const hasLowerCase = /[a-z]/.test(password);
  const hasNumbers = /\d/.test(password);

  if (password.length >= 8 && hasUpperCase && hasLowerCase && hasNumbers) {
    return { 
      isValid: true, 
      message: 'Contraseña fuerte',
      strength: 'strong'
    };
  }

  return { 
    isValid: true, 
    message: 'Contraseña aceptable',
    strength: 'fair'
  };
};

/**
 * Validates username format.
 * 
 * @param {string} username - Username to validate
 * @returns {Object} - { isValid: boolean, message: string }
 */
export const validateUsername = (username) => {
  if (!username || typeof username !== 'string') {
    return { isValid: false, message: 'Nombre de usuario es requerido' };
  }

  const trimmed = username.trim();

  if (trimmed.length < 3) {
    return { 
      isValid: false, 
      message: 'Nombre de usuario debe tener al menos 3 caracteres' 
    };
  }

  if (trimmed.length > 30) {
    return { 
      isValid: false, 
      message: 'Nombre de usuario no puede exceder 30 caracteres' 
    };
  }

  // Only alphanumeric, underscore, and hyphen
  if (!/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
    return { 
      isValid: false, 
      message: 'Solo letras, números, guiones y guiones bajos permitidos' 
    };
  }

  return { isValid: true, message: 'Nombre de usuario válido' };
};

/**
 * Real-time input validator wrapper.
 * Returns validation result based on field type.
 * 
 * @param {string} fieldType - Type of field ('rut', 'email', 'password', 'username')
 * @param {string} value - Value to validate
 * @returns {Object} - Validation result object
 */
export const validateField = (fieldType, value) => {
  switch (fieldType.toLowerCase()) {
    case 'rut':
      return validateRUT(value);
    case 'email':
      return validateEmail(value);
    case 'password':
      return validatePassword(value);
    case 'username':
      return validateUsername(value);
    default:
      return { isValid: true, message: '' };
  }
};

/**
 * Formats date from YYYY-MM-DD to DD/MM/YYYY.
 * Follows Nielsen's Heuristic #2: Match Between System and Real World
 * (Uses Chilean date format convention)
 * 
 * @param {string} dateString - Date in YYYY-MM-DD format
 * @returns {string} - Date in DD/MM/YYYY format
 */
export const formatDate = (dateString) => {
  if (!dateString) return '';
  
  try {
    // Handle both YYYY-MM-DD and ISO date formats
    const date = new Date(dateString + 'T00:00:00');
    
    if (isNaN(date.getTime())) {
      return dateString; // Return original if invalid
    }
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    
    return `${day}/${month}/${year}`;
  } catch (error) {
    return dateString; // Return original on error
  }
};

/**
 * Formats date from YYYY-MM-DD to DD/MM/YYYY HH:mm.
 * Includes time information.
 * 
 * @param {string} dateString - Date/datetime string
 * @returns {string} - Formatted date with time
 */
export const formatDateTime = (dateString) => {
  if (!dateString) return '';
  
  try {
    const date = new Date(dateString);
    
    if (isNaN(date.getTime())) {
      return dateString;
    }
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch (error) {
    return dateString;
  }
};
