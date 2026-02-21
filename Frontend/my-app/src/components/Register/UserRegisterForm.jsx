import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { HelpIcon } from '../Common/Tooltip';
import { validate as validateRutLib, format as formatRutLib, clean as cleanRut } from 'rut.js';
import { validateEmail } from '../../utils/validation';
import { useCtrlEnter } from '../../hooks/useKeyboardShortcuts';

// Icons as constants to keep the JSX clean.
const ICONS = {
  USER: <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" /><path d="M2 14s1-4 6-4 6 4 6 4v1H2v-1z" /></svg>,
  EMAIL: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M3 8l9 6 9-6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /><path d="M21 8v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>,
  PHONE: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M22 16.92V21a1 1 0 0 1-1.11 1 19.86 19.86 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.86 19.86 0 0 1 2 3.11 1 1 0 0 1 3 2h4.09a1 1 0 0 1 1 .75c.12.62.36 1.9.4 2.3a1 1 0 0 1-.24.86L7.7 8.3a15.09 15.09 0 0 0 6 6l1.4-1.4a1 1 0 0 1 .86-.24c.4.04 1.68.28 2.3.4a1 1 0 0 1 .75 1V21z" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" /></svg>,
  ID_CARD: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 2v6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /><path d="M6 8v10a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>,
  USERNAME: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 12c2.761 0 5-2.239 5-5s-2.239-5-5-5-5 2.239-5 5 2.239 5 5 5z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /><path d="M20 21c0-3.866-3.582-7-8-7s-8 3.134-8 7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>,
  LOCK: <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8 1a3 3 0 0 0-3 3v2H4a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-1V4a3 3 0 0 0-3-3zM6 4a2 2 0 1 1 4 0v2H6V4z" /></svg>
};

/**
 * UserRegisterForm component.
 * A reusable form for user registration.
 * 
 * Props:
 * - initial: object with initial values for fields.
 * - requirePassword: boolean (default true) to enforce password fields.
 * - isSuper: boolean to allow selecting ADMIN role.
 * - defaultRole: initial role.
 * - title: heading text.
 * - submitLabel: label for submit button.
 * - onSubmit(form): async function called when validated form is submitted.
 * - onCancel(): optional callback.
 * - hideRoleField: boolean to hide the role selection field.
 * - readOnlyFields: array of field names to be read-only.
 * - isEditMode: boolean to indicate if the form is in edit mode.
 */
const UserRegisterForm = ({
  initial = {},
  requirePassword = true,
  isSuper = false,
  allowedRoles = null,
  defaultRole = 'CLIENT',
  title = 'Crear cuenta',
  submitLabel = 'Crear cuenta',
  onSubmit,
  onCancel,
  hideRoleField = false,
  readOnlyFields = [],
  isEditMode = false,
  isModal = false,
}) => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: initial.username || '',
    name: initial.name || '',
    lastName: initial.lastName || '',
    rut: initial.rut || '',
    phone: initial.phone || '',
    email: initial.email || '',
    password: '',
    confirm: '',
    rol: initial.rol || defaultRole,
  });
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState(null);
  const [validation, setValidation] = useState({
    rut: { isValid: null, message: '' },
    email: { isValid: null, message: '' }
  });

  // Sync form state with initial props when they change
  useEffect(() => {
    setForm(prev => ({
      ...prev,
      username: initial.username || prev.username,
      name: initial.name || prev.name,
      lastName: initial.lastName || prev.lastName,
      rut: initial.rut || prev.rut,
      phone: initial.phone || prev.phone,
      email: initial.email || prev.email,
      rol: initial.rol || prev.rol || defaultRole,
    }));
  }, [initial, defaultRole]);

  // Updates the form state for a given field key.
  const update = (k) => (e) => {
    let value = e.target.value;

    // Auto-format RUT while typing using rut.js library
    if (k === 'rut') {
      // Clean and format the RUT value
      const cleaned = cleanRut(value);
      if (cleaned.length > 0) {
        // Format as user types: XX.XXX.XXX-X
        value = formatRutLib(cleaned);
      }
    }

    setForm({ ...form, [k]: value });

    // Validation for RUT - only validate when appears complete
    if (k === 'rut' && value.trim()) {
      const cleaned = cleanRut(value);
      // Only validate if RUT has at least 7 digits (minimum valid RUT)
      if (cleaned.length >= 7) {
        const isValid = validateRutLib(value);
        setValidation(prev => ({
          ...prev,
          rut: {
            isValid: isValid,
            message: isValid ? 'RUT válido' : 'RUT inválido'
          }
        }));
      } else {
        // RUT incomplete, don't show validation message
        setValidation(prev => ({ ...prev, rut: { isValid: null, message: '' } }));
      }
    } else if (k === 'rut') {
      setValidation(prev => ({ ...prev, rut: { isValid: null, message: '' } }));
    }

    if (k === 'email' && value.trim()) {
      const result = validateEmail(value);
      setValidation(prev => ({
        ...prev,
        email: { isValid: result.isValid, message: result.message }
      }));
    } else if (k === 'email') {
      setValidation(prev => ({ ...prev, email: { isValid: null, message: '' } }));
    }
  };

  // Validates the form fields.
  const validate = () => {
    // Required fields
    if (!form.username || !form.name || !form.lastName || !form.email) {
      setMsg('Por favor completa los campos obligatorios (Nombre, Apellido, Email, Usuario).');
      return false;
    }

    // Email validation
    const emailResult = validateEmail(form.email);
    if (!emailResult.isValid) {
      setMsg(`Email inválido: ${emailResult.message}`);
      return false;
    }

    // RUT validation (if provided)
    if (form.rut && form.rut.trim()) {
      const isValid = validateRutLib(form.rut);
      if (!isValid) {
        setMsg('RUT inválido. Verifica el formato y dígito verificador.');
        return false;
      }
    }

    // Password validation
    if (requirePassword && !isEditMode) {
      if (!form.password) {
        setMsg('Por favor asigna una contraseña (mínimo 6 caracteres).');
        return false;
      }
      if (form.password.length < 6) {
        setMsg('La contraseña debe tener al menos 6 caracteres.');
        return false;
      }
      if (form.password !== form.confirm) {
        setMsg('Las contraseñas no coinciden. Por favor verifica que sean idénticas.');
        return false;
      }
    }
    if (isEditMode && form.password && form.password !== form.confirm) {
      setMsg('Las contraseñas no coinciden. Por favor verifica que sean idénticas.');
      return false;
    }

    return true;
  };

  // Handles form submission.
  const submit = async (e) => {
    e && e.preventDefault();
    setMsg(null);
    if (!validate()) return;
    setLoading(true);
    try {
      if (onSubmit) await onSubmit(form);
    } catch (err) {
      // Enhanced error message with recovery suggestions (Nielsen Heuristic #9)
      let errorMsg = err?.response?.data?.message || err?.response?.data || err?.message || 'Error al procesar la solicitud.';

      // Add specific recovery suggestions based on error type
      if (err?.response?.status === 409) {
        errorMsg = 'El email o nombre de usuario ya está registrado. Por favor usa uno diferente.';
      } else if (err?.response?.status === 400) {
        errorMsg = 'Datos inválidos. ' + errorMsg + ' Verifica que todos los campos sean correctos.';
      } else if (err?.response?.status === 403) {
        errorMsg = 'No tienes permisos para realizar esta operación. Contacta al administrador.';
      } else if (err?.response?.status === 500) {
        errorMsg = 'Error del servidor. Por favor intenta nuevamente en unos momentos.';
      } else if (!err?.response) {
        errorMsg = 'No se pudo conectar con el servidor. Verifica tu conexión a internet e intenta nuevamente.';
      }

      setMsg(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const isReadOnly = (field) => readOnlyFields.includes(field);

  // Keyboard shortcut: Ctrl+Enter to submit
  useCtrlEnter(submit, !loading);

  return (
    <div className={isModal ? '' : 'form-card'} style={isModal ? {} : { maxWidth: 900 }}>
      <h2 className="form-title">{title}</h2>
      <p className="form-sub">{isEditMode ? 'Configuración de usuario' : 'Crea una cuenta en el sistema'}</p>

      <form onSubmit={submit} className="form-grid">
        <div className="input-group">
          <label>Nombre</label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.USER}</span>
            <input value={form.name} onChange={update('name')} placeholder="Nombre" required disabled={isReadOnly('name')} />
          </div>
        </div>

        <div className="input-group">
          <label>Apellido</label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.USER}</span>
            <input value={form.lastName} onChange={update('lastName')} placeholder="Apellido" required disabled={isReadOnly('lastName')} />
          </div>
        </div>

        <div className="input-group">
          <label>
            Email <HelpIcon content="Correo electrónico para iniciar sesión" position="top" />
          </label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.EMAIL}</span>
            <input
              type="email"
              value={form.email}
              onChange={update('email')}
              placeholder="tu@ejemplo.com"
              required
              disabled={isReadOnly('email')}
              style={{
                borderColor: validation.email.isValid === false ? '#ef4444' : validation.email.isValid === true ? 'limegreen' : undefined
              }}
            />
          </div>
          {validation.email.message && (
            <small style={{ color: validation.email.isValid ? 'limegreen' : '#ef4444', fontSize: '0.8rem', marginTop: '4px' }}>
              {validation.email.message}
            </small>
          )}
        </div>

        <div className="input-group">
          <label>Teléfono</label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.PHONE}</span>
            <input value={form.phone} onChange={update('phone')} placeholder="+56 9 1234 5678" disabled={isReadOnly('phone')} />
          </div>
        </div>

        <div className="input-group">
          <label>
            RUT <HelpIcon content="Ejemplo: 12.345.678-9" position="top" />
          </label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.ID_CARD}</span>
            <input
              value={form.rut}
              onChange={update('rut')}
              placeholder="12.345.678-9"
              disabled={isReadOnly('rut')}
              maxLength={12}
              style={{
                borderColor: validation.rut.isValid === false ? '#ef4444' : validation.rut.isValid === true ? 'limegreen' : undefined
              }}
            />
          </div>
          {validation.rut.message && (
            <small style={{ color: validation.rut.isValid ? 'limegreen' : '#ef4444', fontSize: '0.8rem', marginTop: '4px' }}>
              {validation.rut.message}
            </small>
          )}
        </div>

        <div className="input-group">
          <label>Usuario</label>
          <div className="input-with-icon">
            <span className="icon" aria-hidden>{ICONS.USERNAME}</span>
            <input value={form.username} onChange={update('username')} placeholder="nombre de usuario" required disabled={isReadOnly('username')} />
          </div>
        </div>

        {requirePassword && (
          <>
            <div className="input-group">
              <label>Contraseña {isEditMode && '(Opcional)'}</label>
              <div className="input-with-icon">
                <span className="icon" aria-hidden>{ICONS.LOCK}</span>
                <input type="password" value={form.password} onChange={update('password')} placeholder={isEditMode ? "Dejar en blanco para no cambiar" : "Contraseña"} required={!isEditMode} />
              </div>
            </div>

            <div className="input-group">
              <label>Repetir contraseña</label>
              <div className="input-with-icon">
                <span className="icon" aria-hidden>{ICONS.LOCK}</span>
                <input type="password" value={form.confirm} onChange={update('confirm')} placeholder="Repite la contraseña" required={!isEditMode || form.password.length > 0} />
              </div>
            </div>
          </>
        )}

        {!hideRoleField && (
          <div className="input-group">
            <label>Rol</label>
            <div className="input-with-icon">
              {/* Render roles based on allowedRoles prop; fallback: if isSuper allow ADMIN/EMPLOYEE only */}
              {Array.isArray(allowedRoles) && allowedRoles.length > 0 ? (
                <select value={form.rol} onChange={update('rol')} disabled={isReadOnly('rol')}>
                  {allowedRoles.map((r) => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
              ) : isSuper ? (
                <select value={form.rol} onChange={update('rol')} disabled={isReadOnly('rol')}>
                  <option value="EMPLOYEE">EMPLOYEE</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              ) : (
                <select value={form.rol} onChange={update('rol')} disabled={isReadOnly('rol')}>
                  <option value={form.rol}>{form.rol}</option>
                </select>
              )}
            </div>
          </div>
        )}

        <div className="actions">
          <button className="primary-cta" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
            {loading ? 'Guardando...' : submitLabel}
          </button>
          {!isEditMode && !isModal && (
            <button type="button" className="link" style={{ width: '100%', justifyContent: 'center' }} onClick={() => navigate('/login')}>¿Ya tienes cuenta? Inicia sesión</button>
          )}
          {onCancel && <button type="button" className="link" onClick={onCancel} style={{ width: '100%', justifyContent: 'center' }}>Cancelar</button>}
        </div>

        {msg && <div className="form-message">{msg}</div>}
      </form>
    </div>
  );
};

export default UserRegisterForm;
