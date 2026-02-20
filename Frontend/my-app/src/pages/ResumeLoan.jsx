import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import api from '../services/http-common';
import TransitionAlert from '../components/Alerts/TransitionAlert';

const ResumeLoan = () => {
  const navigate = useNavigate();
  const [resume, setResume] = useState(null);
  const [initDate, setInitDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const [creating, setCreating] = useState(false);
  const [alert, setAlert] = useState(null);
  const [dateError, setDateError] = useState('');
  const [total, setTotal] = useState(0);
  const [countdown, setCountdown] = useState(null);
  
    const isDatesValid = () => {
        if (!initDate || !returnDate) return false;
        try {
          const dInit = new Date(initDate);
          const dRet = new Date(returnDate);
          const minDiff = 24 * 60 * 60 * 1000;
          return (dRet.getTime() - dInit.getTime()) >= minDiff;
        } catch (e) {
          return false;
        }
    };

  // derived validation flag used to enable/disable the Confirm button
  const datesValid = isDatesValid();

  // Countdown effect for redirect after order creation
  useEffect(() => {
    if (countdown === null) return;
    if (countdown === 0) {
      navigate('/');
      return;
    }
    const timer = setTimeout(() => {
      const nextCount = countdown - 1;
      setCountdown(nextCount);
      if (nextCount > 0) {
        setAlert({ severity: 'success', message: `Pedido creado y herramientas prestadas correctamente. Redirigiendo al inicio en ${nextCount}...` });
      }
    }, 1000);
    return () => clearTimeout(timer);
  }, [countdown, navigate]);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem('order_resume');
      if (raw) {
        const data = JSON.parse(raw);
        setResume(data);
        
        // Load dates from sessionStorage (saved in OrdersCreateClient)
        const savedInitDate = sessionStorage.getItem('order_init_date');
        const savedReturnDate = sessionStorage.getItem('order_return_date');
        
        if (savedInitDate && savedReturnDate) {
          setInitDate(savedInitDate);
          setReturnDate(savedReturnDate);
        } else {
          // Fallback: sensible defaults
          const today = new Date();
          const iso = d => d.toISOString().slice(0,10);
          const init = iso(today);
          setInitDate(init);
          const plus1 = new Date(today.getTime() + 1*24*60*60*1000);
          setReturnDate(iso(plus1));
        }
      }
    } catch (e) {
      console.warn('Could not read resume data', e);
    }
  }, []);

  const calcTotal = () => {
    if (total && Number(total) > 0) return Number(total);
    if (!resume || !Array.isArray(resume.items)) return 0;
    return resume.items.reduce((s,it) => s + (Number(it.price || 0) * Number(it.qty || 1)), 0);
  };

  const confirmAndCreate = async () => {
    if (!resume || !resume.client || !resume.items || resume.items.length === 0) {
      setAlert({ 
        severity: 'error', 
        message: 'Faltan datos del pedido. Verifica que hayas seleccionado un cliente y agregado herramientas. Regresa al paso anterior para completar la información.' 
      });
      return;
    }
    if (!initDate || !returnDate) { 
      setAlert({ 
        severity: 'error', 
        message: 'Debes seleccionar ambas fechas: inicio y devolución. La fecha de devolución debe ser al menos 1 día después de la fecha inicial.'
      }); 
      return; 
    }

    // Validate dates: returnDate must be at least one day after initDate
    const dInit = new Date(initDate);
    const dReturn = new Date(returnDate);
    const diffMs = dReturn.getTime() - dInit.getTime();
    const minDiff = 24 * 60 * 60 * 1000; // 1 day
    if (diffMs < minDiff) {
      setAlert({ 
        severity: 'error', 
        message: 'La fecha de devolución debe ser al menos 1 día después de la fecha inicial. Ajusta las fechas e intenta nuevamente.'
      });
      return;
    }
    
    // Final validation: ensure dates are valid before sending
    if (!isDatesValid()) {
      setAlert({ 
        severity: 'error', 
        message: 'Fechas inválidas. La fecha inicial debe ser hoy y la devolución al menos 1 día después. Verifica las fechas seleccionadas.'
      });
      return;
    }

    setCreating(true);
    try {
      // Get employee ID
      const employeeResp = await api.get('/user/me');
      const employeeId = employeeResp.data?.id;
      if (!employeeId) throw new Error('No se pudo obtener tu identificación de empleado. Verifica que hayas iniciado sesión correctamente. Si el problema persiste, contacta al administrador.');

      // Extract tool IDs from resume items
      const toolIds = resume.items.map(it => Number(it.id));

      // Call atomic endpoint to create Loan + LoanXTools in one transaction
      const payload = {
        clientId: resume.client.id,
        initDate: initDate,
        returnDate: returnDate,
        toolIds: toolIds
      };

      const createResp = await api.post(`/loan/create-with-tools/${employeeId}`, payload);
      const createdLoan = createResp.data;

      if (!createdLoan || !createdLoan.id) {
        throw new Error('No se recibió confirmación del servidor. El préstamo pudo no haberse creado correctamente. Verifica en la lista de préstamos si se registró o intenta nuevamente.');
      }

      // Now mark all tools as 'PRESTADA' (given to client)
      // First, we need to get the LoanXTools IDs for this loan
      const lxtResp = await api.get(`/loantool/loan/${createdLoan.id}`);
      const loanXTools = lxtResp.data || [];
      const loanXToolIds = loanXTools.map(lxt => lxt.id);

      if (loanXToolIds.length === 0) {
        throw new Error('El préstamo se creó pero no se encontraron herramientas asociadas. Verifica el préstamo en la lista de préstamos o contacta al administrador.');
      }

      // Call batch give endpoint to mark all as PRESTADA
      await api.post(`/loantool/give/all/user/${employeeId}`, loanXToolIds);

      // Success: clear all session data
      sessionStorage.removeItem('order_resume');
      sessionStorage.removeItem('order_selected_client');
      sessionStorage.removeItem('order_loan_id');
      sessionStorage.removeItem('order_items');
      sessionStorage.removeItem('order_init_date');
      sessionStorage.removeItem('order_return_date');
      
      setAlert({ severity: 'success', message: 'Pedido creado y herramientas prestadas correctamente. Redirigiendo al inicio en 5...' });
      setCountdown(5);
      
    } catch (e) {
      console.error('Error creating order', e?.response?.data || e.message || e);
      const backendMsg = e?.response?.data && (e.response.data.error || e.response.data.message || e.response.data);
      
      // Enhanced error message with recovery suggestions
      let errorMsg = backendMsg || e.message || 'Error al crear el pedido.';
      
      // Add recovery suggestions based on error type
      if (e?.response?.status === 409) {
        errorMsg += ' Una o más herramientas ya están prestadas. Verifica el inventario e intenta con otras herramientas.';
      } else if (e?.response?.status === 400) {
        errorMsg += ' Los datos enviados son inválidos. Verifica las fechas, el cliente seleccionado y las herramientas agregadas.';
      } else if (e?.response?.status === 404) {
        errorMsg += ' No se encontró el recurso solicitado. Verifica que el cliente y las herramientas todavía existan en el sistema.';
      } else if (e?.response?.status === 500) {
        errorMsg += ' Error interno del servidor. Por favor intenta nuevamente en unos momentos o contacta al administrador.';
      } else if (!e?.response) {
        errorMsg = 'No se pudo conectar con el servidor. Verifica tu conexión a internet e intenta nuevamente.';
      }
      
      setAlert({ severity: 'error', message: errorMsg });
    } finally {
      setCreating(false);
    }
  };

  if (!resume) return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      {alert && <TransitionAlert alert={alert} onClose={() => setAlert(null)} autoHideMs={countdown !== null ? 0 : 4000} />}
      <main className="px-6">
        <div className="max-w-4xl mx-auto">No hay datos del pedido. Vuelve y selecciona cliente/herramientas.</div>
      </main>
    </div>
  );

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      {alert && <TransitionAlert alert={alert} onClose={() => setAlert(null)} autoHideMs={countdown !== null ? 0 : 4000} />}
      <main className="px-6">
        <div className="max-w-4xl mx-auto">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <div>
              <h2 style={{ margin: 0 }}>Resumen del Pedido</h2>
              <p style={{ margin: '4px 0 0', color: '#4b5563' }}>Revisa los datos antes de confirmar el pedido.</p>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
              <BackButton onClick={() => {
                // volver al paso anterior del flujo de creación de pedido
                navigate('/admin/orders/create/tools');
              }} />
            </div>
          </div>

          <div style={{ marginTop: 12, display: 'flex', gap: 16 }}>
            <section style={{ flex: '0 0 320px', background: '#fff', padding: 12, borderRadius: 8, border: '1px solid #e6e6e6' }}>
              <h4>Cliente</h4>
              <div style={{ marginTop: 8 }}>
                <div style={{ fontWeight: 800 }}>{resume.client.name ? `${resume.client.name} ${resume.client.lastName || ''}` : (resume.client.username || resume.client.email)}</div>
                <div style={{ marginTop: 4, fontSize: 16, color: '#374151' }}>{resume.client.username || resume.client.email}</div>
                {resume.client.rut && <div style={{ marginTop: 4, fontSize: 16 }}>RUT: {resume.client.rut}</div>}
              </div>
            </section>

            <section style={{ flex: 1, background: '#fff', padding: 12, borderRadius: 8, border: '1px solid #e6e6e6' }}>
              <h4>Items</h4>
              <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
                {resume.items.map(it => (
                  <div key={it.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 8, borderRadius: 6, background: '#fafafa' }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                      <img src={it.image || 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80'} alt={it.name} style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 6 }} />
                      <div>
                        <div style={{ fontWeight: 700 }}>{it.name}</div>
                        <div style={{ color: '#64748b' }}>Cantidad: {it.qty}</div>
                      </div>
                    </div>
                    <div style={{ fontWeight: 700 }}>${(Number(it.price || 0) * Number(it.qty || 1)).toLocaleString()}</div>
                  </div>
                ))}
              </div>

              <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: 20, fontWeight: 800, color: '#334155' }}>Total</div>
                <div style={{ fontSize: 20, fontWeight: 800 }}>${calcTotal().toLocaleString()}</div>
              </div>

              <div style={{ marginTop: 16 }}>
                <label>Fecha inicio: <input type="date" value={initDate} min={initDate} disabled /></label>
                <label style={{ marginLeft: 12 }}>Fecha retorno: <input type="date" value={returnDate} min={(() => {
                    try { const d = new Date(initDate); d.setDate(d.getDate() + 1); return d.toISOString().slice(0,10); } catch(e) { return '' }
                  })()} disabled /></label>
                {dateError && <div style={{ color: '#b91c1c', marginTop: 6 }}>{dateError}</div>}
              </div>

              <div style={{ marginTop: 16, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                <button className="primary-cta" onClick={confirmAndCreate} disabled={creating || !datesValid}>{creating ? 'Creando...' : 'Confirmar pedido'}</button>
              </div>
            </section>
          </div>

          {/* alert moved to top (below NavBar) */}
        </div>
      </main>
    </div>
  );
};

export default ResumeLoan;
