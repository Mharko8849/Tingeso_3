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
    // sensible defaults: init = today, return = tomorrow
    try {
      const today = new Date();
      const iso = d => d.toISOString().slice(0,10);
      setInitDate(iso(today));
      const plus1 = new Date(today.getTime() + 1*24*60*60*1000);
      setReturnDate(iso(plus1));
    } catch(e) { /* ignore */ }
  }, []);

  const isDatesValid = () => {
    if (!initDate || !returnDate) return false;
    try {
      const dInit = new Date(initDate);
      const dRet = new Date(returnDate);
      return dRet.getTime() - dInit.getTime() >= 24*60*60*1000;
    } catch(e) { return false; }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main style={{ paddingTop: 30 }} className="px-6">
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
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z" fill="#fff"/></svg>
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
                      // Use same registration endpoint as ClientsAdmin
                      const res = await fetch('/api/auth/register', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                          username: payload.username,
                          name: payload.name,
                          lastName: payload.lastName,
                          rut: payload.rut,
                          phone: payload.phone,
                          email: payload.email,
                          password: payload.password,
                          rol: payload.rol || 'CLIENT',
                        }),
                      });
                      if (!res.ok) {
                        const err = await res.text();
                        throw new Error(err || 'Registro de cliente falló');
                      }
                      const created = await res.json();
                      const toAppend = created || { username: payload.username, name: payload.name, lastName: payload.lastName, email: payload.email, rol: payload.rol || 'CLIENT', rut: payload.rut };
                      // set as selected and store in session
                      setSelected(toAppend);
                      // trigger client list reload so ClientSearch refetches and shows the new client
                      setClientListReload((s) => s + 1);
                      try { 
                        sessionStorage.setItem('order_selected_client', JSON.stringify(toAppend)); 
                        // clear any previous pending order data
                        sessionStorage.removeItem('order_items');
                        sessionStorage.removeItem('order_resume');
                        sessionStorage.removeItem('order_loan_id');
                      } catch (e) {}
                      setShowRegister(false);
                      show({ severity: 'success', message: 'La cuenta ha sido registrada exitosamente.' });
                    } catch (e) {
                      // rethrow so EmployeeRegister/UserRegisterForm shows the error and also show page alert
                      setShowRegister(true);
                      show({ severity: 'error', message: e?.message || 'Error al crear cliente' });
                      throw e;
                    }
                  }}
                  onCancel={() => setShowRegister(false)}
                />
              )}

              {/* ClientSearch handles its own scrolling and card grid (hide internal label since header above provides it) */}
              <ClientSearch selected={selected} onSelect={setSelected} reloadKey={clientListReload} hideHeader={true} />

              {/* Dates moved here so they appear under the client list/table */}
              <div style={{ marginTop: 12, display: 'flex', gap: 24, alignItems: 'center', justifyContent: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <label style={{ display: 'block', fontSize: 13 }}>Fecha inicio</label>
                  <input type="date" value={initDate} onChange={(e) => setInitDate(e.target.value)} />
                </div>
                <div style={{ textAlign: 'center' }}>
                  <label style={{ display: 'block', fontSize: 13 }}>Fecha retorno</label>
                  <input type="date" value={returnDate} onChange={(e) => setReturnDate(e.target.value)} />
                </div>
              </div>
            </div>
          </section>

          <aside style={{ position: 'relative' }}>
            <div style={{ position: 'sticky', top: 90 }}>
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
                        } catch (e) {}
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
