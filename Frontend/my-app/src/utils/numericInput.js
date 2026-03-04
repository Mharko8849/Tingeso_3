/**
 * Validates and handles numeric input for form fields.
 * Only accepts empty strings or positive integers.
 * @param {string} field - Form field name
 * @param {string} value - Input value
 * @param {function} setForm - State setter for the form
 * @param {object} alert - Alert context with show() method
 */
export const handleNumericInput = (field, value, setForm, alert) => {
  if (value === '' || /^\d+$/.test(value)) {
    setForm((s) => ({ ...s, [field]: value }));
  } else {
    alert?.show?.({ severity: 'warning', message: 'Debe ingresar valores enteros positivos', autoHideMs: 3500 });
  }
};
