import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import ClientSearch from '../components/Clients/ClientSearch';
import EmployeeRegister from '../components/Register/ModalEmployeesRegister';
import api from '../services/http-common';
import { useAlert } from '../components/Alerts/useAlert';
import { useKeycloak } from '@react-keycloak/web';

const OrdersCreateClient = () => {
  const navigate = useNavigate();
  const [selected, setSelected] = useState(null);
  const [showRegister, setShowRegister] = useState(false);
  const [clientListReload, setClientListReload] = useState(0);
  const [creating, setCreating] = useState(false);
  const [initDate, setInitDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const { show } = useAlert();

  // Lógica basada en Keycloak + token local de respaldo
  const { keycloak } = useKeycloak();
  const parseJwt = (token) => {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    } catch (e) { return null; }
  };
  let legacyRoles = [];
  if (keycloak?.tokenParsed?.realm_access?.roles) {
    legacyRoles = keycloak.tokenParsed.realm_access.roles;
  } else {
    const localToken = typeof window !== 'undefined' ? (localStorage.getItem('access_token') || localStorage.getItem('app_token')) : null;
    if (localToken) {
      const p = parseJwt(localToken);
      if (p && p.realm_access && Array.isArray(p.realm_access.roles)) legacyRoles = p.realm_access.roles;
    }
  }
  legacyRoles = legacyRoles.map((r) => String(r).toUpperCase());
  const isSuper = legacyRoles.includes('SUPERADMIN');
  const isAdmin = legacyRoles.includes('ADMIN');

  const goBack = () => {
    navigate('/');
  };

  const handleClientSelect = (client) => {
    // Validar si el cliente está restringido
    if (client && client.stateClient === 'RESTRINGIDO') {
      show({
        severity: 'error',
        message: 'El usuario seleccionado se encuentra actualmente restringido.',
        autoHideMs: 4500
      });
      return; // No permitir la selección
    }
    // Si no está restringido, permitir la selección
    setSelected(client);
  };

  const handleNext = () => {
    if (!selected) return;
    // Save client and dates to sessionStorage (NO loan creation here)
    (async () => {
      setCreating(true);
      try {
        // Store selected client and dates in session for next step
        try {
          sessionStorage.setItem('order_selected_client', JSON.stringify(selected));
          sessionStorage.setItem('order_init_date', initDate);
          sessionStorage.setItem('order_return_date', returnDate);
          // Clear any previous items/resume from earlier incomplete orders
          sessionStorage.removeItem('order_items');
          sessionStorage.removeItem('order_resume');
          sessionStorage.removeItem('order_loan_id'); // No loan ID yet
        } catch (e) { console.warn('sessionStorage not available', e); }

        // Navigate to tools step
        navigate('/admin/orders/create/tools');
      } catch (e) {
        console.warn('Error al preparar el pedido', e?.response?.data || e.message || e);
        show({ severity: 'error', message: 'No se pudo preparar el pedido. Intenta nuevamente.' });
      } finally {
        setCreating(false);
      }
    })();
  };

  useEffect(() => {
    // sensible defaults: init = today, return = tomorrow (LOCAL timezone)
    try {
      setInitDate(getTodayString());
      setReturnDate(getTomorrowString());
    } catch (e) { /* ignore */ }
  }, []);

  const isDatesValid = () => {
    if (!initDate || !returnDate) return false;
    try {
      const dInit = new Date(initDate);
      const dRet = new Date(returnDate);
      return dRet.getTime() - dInit.getTime() >= 24 * 60 * 60 * 1000;
    } catch (e) { return false; }
  };

  // Helper: Get today's date in YYYY-MM-DD format (LOCAL timezone, not UTC)
  const getTodayString = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // Helper: Get tomorrow's date in YYYY-MM-DD format (LOCAL timezone)
  const getTomorrowString = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const year = tomorrow.getFullYear();
    const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const day = String(tomorrow.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // Auto-correct dates when user selects invalid values
  const handleInitDateChange = (e) => {
    const selectedDate = e.target.value;
    const today = getTodayString();

    // Si selecciona fecha pasada, establecer hoy
    if (selectedDate < today) {
      setInitDate(today);
      show({ severity: 'warning', message: 'No puedes seleccionar una fecha pasada. Se estableció la fecha de hoy.' });
    } else {
      setInitDate(selectedDate);

      // Si la fecha de retorno ahora es anterior o igual a la de inicio, ajustarla
      if (returnDate <= selectedDate) {
        const newReturn = new Date(selectedDate + 'T00:00:00');
        newReturn.setDate(newReturn.getDate() + 1);
        const year = newReturn.getFullYear();
        const month = String(newReturn.getMonth() + 1).padStart(2, '0');
        const day = String(newReturn.getDate()).padStart(2, '0');
        const newReturnStr = `${year}-${month}-${day}`;
        setReturnDate(newReturnStr);
        show({ severity: 'info', message: 'Fecha de retorno ajustada para ser posterior a la fecha de inicio.' });
      }
    }
  };

  const handleReturnDateChange = (e) => {
    const selectedDate = e.target.value;

    // La fecha de retorno debe ser al menos 1 día después de la de inicio
    if (!initDate) {
      // Si no hay fecha de inicio, establecer defaults
      const today = getTodayString();
      setInitDate(today);
      setReturnDate(getTomorrowString());
      show({ severity: 'info', message: 'Se establecieron fechas por defecto.' });
      return;
    }

    // Si selecciona fecha anterior o igual a la de inicio, ajustar a inicio + 1 día
    if (selectedDate <= initDate) {
      const minReturn = new Date(initDate + 'T00:00:00');
      minReturn.setDate(minReturn.getDate() + 1);
      const year = minReturn.getFullYear();
      const month = String(minReturn.getMonth() + 1).padStart(2, '0');
      const day = String(minReturn.getDate()).padStart(2, '0');
      const minReturnStr = `${year}-${month}-${day}`;
      setReturnDate(minReturnStr);
      show({ severity: 'warning', message: 'La fecha de retorno debe ser al menos 1 día después de la fecha de inicio.' });
    } else {
      setReturnDate(selectedDate);
    }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="max-w-7xl mx-auto" style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 24 }}>
          <section style={{ paddingRight: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <div>
                <h2 style={{ margin: 0 }}>Crear Pedido — Selecciona cliente</h2>
                <p style={{ margin: '4px 0 0', color: '#4b5563' }}>Busca y selecciona el cliente al que se asociará el pedido. Luego selecciona las fechas y presiona "Siguiente".</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
                <BackButton onClick={goBack} />
              </div>
            </div>

            {/* Date selectors moved below client list to keep search first (see design request) */}

            <div style={{ marginTop: 12 }}>
              {/* Header row: label left, add-client button right (button aligned to the far right) */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                <label style={{ fontSize: 16, fontWeight: 700 }}>Buscar cliente</label>
                <div>
                  <button onClick={() => setShowRegister((s) => !s)} className="primary-cta" type="button">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z" fill="#fff" /></svg>
                    <span style={{ marginLeft: 8 }}>Añadir Cliente</span>
                  </button>
                </div>
              </div>

              {showRegister && (
                <EmployeeRegister
                  title="Añadir cliente"
                  defaultRole="CLIENT"
                  allowedRoles={["CLIENT"]}
                  hideRoleField={true}
                  isSuper={isSuper}
                  isAdmin={isAdmin}
                  onCreate={async (payload) => {
                    try {
                      // Use axios api instead of fetch to properly route through interceptors
                      const res = await api.post('/api/auth/register', {
                        username: payload.username,
                        name: payload.name,
                        lastName: payload.lastName,
                        rut: payload.rut,
                        phone: payload.phone,
                        email: payload.email,
                        password: payload.password,
                        rol: payload.rol || 'CLIENT',
                      });
                      const created = res.data;
                      const toAppend = created || { username: payload.username, name: payload.name, lastName: payload.lastName, email: payload.email, rol: payload.rol || 'CLIENT', rut: payload.rut };
                      // set as selected and store in session
                      // Note: Newly registered clients should be in ACTIVO state by default, so no need to validate here
                      setSelected(toAppend);
                      // trigger client list reload so ClientSearch refetches and shows the new client
                      setClientListReload((s) => s + 1);
                      try {
                        sessionStorage.setItem('order_selected_client', JSON.stringify(toAppend));
                        // clear any previous pending order data
                        sessionStorage.removeItem('order_items');
                        sessionStorage.removeItem('order_resume');
                        sessionStorage.removeItem('order_loan_id');
                      } catch (e) { }
                      setShowRegister(false);
                      show({ severity: 'success', message: 'La cuenta ha sido registrada exitosamente.' });
                    } catch (e) {
                      // Show user-friendly error message instead of HTML
                      const errorMessage = e?.response?.data?.error
                        || e?.response?.data?.message
                        || e?.message
                        || 'No se pudo crear el cliente. Por favor verifica los datos e intenta nuevamente.';

                      console.error('Error creating client:', e);
                      setShowRegister(true);
                      show({ severity: 'error', message: errorMessage });
                      throw new Error(errorMessage);
                    }
                  }}
                  onCancel={() => setShowRegister(false)}
                />
              )}

              {/* ClientSearch handles its own scrolling and card grid (hide internal label since header above provides it) */}
              <ClientSearch selected={selected} onSelect={handleClientSelect} reloadKey={clientListReload} hideHeader={true} />
            </div>
          </section>

          <aside style={{ position: 'relative' }}>
            <div style={{ position: 'sticky', top: 90, display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Dates selector box */}
              <div style={{ padding: 18, borderRadius: 8, background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)', border: '1px solid #eee' }}>
                <h3 style={{ marginTop: 0, marginBottom: 16 }}>Fechas</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Fecha inicio</label>
                    <input
                      type="date"
                      value={initDate}
                      min={getTodayString()}
                      onChange={handleInitDateChange}
                      style={{ width: '100%', padding: '6px 10px', fontSize: 14, borderRadius: 6, border: '1px solid #d1d5db' }}
                    />
                    <div style={{ fontSize: 11, color: '#6b7280', marginTop: 4 }}>Mínimo: Hoy</div>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Fecha retorno</label>
                    <input
                      type="date"
                      value={returnDate}
                      min={initDate ? (() => {
                        const minReturn = new Date(initDate + 'T00:00:00');
                        minReturn.setDate(minReturn.getDate() + 1);
                        const year = minReturn.getFullYear();
                        const month = String(minReturn.getMonth() + 1).padStart(2, '0');
                        const day = String(minReturn.getDate()).padStart(2, '0');
                        return `${year}-${month}-${day}`;
                      })() : getTomorrowString()}
                      onChange={handleReturnDateChange}
                      style={{ width: '100%', padding: '6px 10px', fontSize: 14, borderRadius: 6, border: '1px solid #d1d5db' }}
                    />
                    <div style={{ fontSize: 11, color: '#6b7280', marginTop: 4 }}>
                      Mínimo: {initDate ? 'Inicio + 1 día' : 'Mañana'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Selected client box */}
              <div style={{ padding: 18, borderRadius: 8, background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)', border: '1px solid #eee' }}>
                <h3 style={{ marginTop: 0 }}>Seleccionado</h3>
                {selected ? (
                  <div>
                    <div style={{ fontWeight: 700 }}>{selected.username}</div>
                    <div style={{ fontSize: 13 }}>{selected.name} {selected.lastName}</div>
                    <div style={{ fontSize: 13 }}>{selected.email}</div>
                    {selected.rut && <div style={{ marginTop: 8, fontSize: 13 }}>RUT: {selected.rut}</div>}
                    {selected.stateClient && <div style={{ marginTop: 6, fontSize: 13 }}>Estado: {selected.stateClient}</div>}

                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 16 }}>
                      <button className="link" onClick={() => {
                        // Deselect locally and clear any session draft related to client
                        setSelected(null);
                        try {
                          sessionStorage.removeItem('order_selected_client');
                          sessionStorage.removeItem('order_items');
                          sessionStorage.removeItem('order_loan_id');
                          sessionStorage.removeItem('order_resume');
                        } catch (e) { }
                      }}>Deseleccionar</button>
                      <button className="primary-cta" onClick={handleNext} disabled={!selected || creating || !isDatesValid()}>{creating ? 'Creando...' : 'Siguiente'}</button>
                    </div>
                  </div>
                ) : (
                  <div style={{ color: '#666' }}>
                    No hay cliente seleccionado

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
                      <button className="link" onClick={goBack}>Cancelar</button>
                      <button className="primary-cta" onClick={handleNext} disabled={!selected || creating || !isDatesValid()}>{creating ? 'Creando...' : 'Siguiente'}</button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
};

export default OrdersCreateClient;
