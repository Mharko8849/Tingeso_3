import React, { useEffect, useState } from 'react';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import ReturnToolCard from '../components/Returns/ReturnToolCard';
import LoadingSpinner from '../components/Loading/LoadingSpinner';
import api from '../services/http-common';
import Badge from '../components/Badges/Badge';
import { statusToBadgeVariant } from '../components/Badges/statusToBadge';
import { useAlert } from '../components/Alerts/useAlert';
import RepairPaymentModal from '../components/Returns/RepairPaymentModal';
import DebtPaymentModal from '../components/Returns/DebtPaymentModal';
import { formatDate } from '../utils/validation';
import PaginationBar from '../components/Common/PaginationBar';

const parseLoanIdFromPath = (pathname) => {
  const parts = pathname.split('/').filter(Boolean);
  const str = parts[parts.length - 1];
  if (!str) { return null; }
  const num = Number(str);
  if (!Number.isFinite(num) || num <= 0) { return null; }
  return str;
};

const getClientDisplayName = (user) => {
  if (!user) { return ''; }
  if (user.name) { return `${user.name} ${user.lastName || ''}`; }
  return user.username || user.email || '';
};

const showValidationError = (alertFn, count) => {
  try { alertFn?.show?.({ severity: 'warning', message: `Falta seleccionar estado para ${count} herramienta(s)`, autoHideMs: 4000 }); } catch (error_) { console.debug(error_); }
};

const showSuccessAlert = (alertFn) => {
  try { alertFn?.show?.({ severity: 'success', message: 'Devolución completada correctamente.', autoHideMs: 3000 }); } catch (error_) { console.debug(error_); }
};

const showReturnError = (alertFn, err) => {
  const resp = err?.response?.data;
  try {
    let msg;
    if (resp) {
      msg = typeof resp === 'string' ? resp : JSON.stringify(resp);
    } else {
      msg = err?.message || 'Error desconocido';
    }
    alertFn?.show?.({ severity: 'error', message: msg, autoHideMs: 8000 });
  } catch (error_) { console.debug(error_); }
};

const ReturnsLoanSummary = () => {
  const [loan, setLoan] = useState(null);
  const [items, setItems] = useState([]);
  const [stateToolMap, setStateToolMap] = useState({});
  const [returning, setReturning] = useState(false);
  const alert = useAlert();
  const statusUpper = String(loan?.status || '').toUpperCase();
  const isActive = statusUpper === 'ACTIVO' || statusUpper === 'ACTIVE';
  const [totalFine, setTotalFine] = useState(0);
  const payingDebt = false;
  const [showRepairModal, setShowRepairModal] = useState(false);
  const [showDebtModal, setShowDebtModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    const loanId = parseLoanIdFromPath(globalThis.location.pathname);
    if (!loanId) {
      try { alert?.show?.({ severity: 'error', message: 'ID de pedido inválido en la URL', autoHideMs: 4000 }); } catch (error_) { console.debug(error_); }
      setLoading(false);
      return;
    }
    api.get(`/api/loan/${loanId}`)
      .then(res => setLoan(res.data))
      .catch(() => setLoan(null));
    api.get(`/api/loantool/total/fine/${loanId}`)
      .then(res => setTotalFine(Number(res.data || 0)))
      .catch(() => setTotalFine(0));
    api.get(`/api/loantool/loan/${loanId}`)
      .then(res => setItems(res.data || []))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const total = items.length;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const safePage = Math.min(Math.max(1, page), totalPages);
  const start = (safePage - 1) * pageSize;
  const end = start + pageSize;
  const pagedItems = items.slice(start, end);

  const handleReturn = async () => {
    if (!loan) { return; }
    setReturning(true);
    try {
      const missing = items.filter(it => !stateToolMap[it.id]);
      if (missing.length > 0) {
        showValidationError(alert, missing.length);
        setReturning(false);
        return;
      }
      const payload = {};
      items.forEach(it => { payload[it.id] = stateToolMap[it.id]; });
      const meResp = await api.get('/api/user/me');
      const employeeId = meResp?.data?.id;
      if (!employeeId) { throw new Error('No pude obtener tu id de usuario (employee)'); }
      await api.post(`/api/loantool/receive/all/loan/${loan.id}/user/${employeeId}`, payload);
      showSuccessAlert(alert);
      await api.get(`/api/loantool/loan/${loan.id}`).then(r => setItems(r.data || []));
      await api.get(`/api/loan/${loan.id}`).then(r => setLoan(r.data)).catch(() => {});
      try { const tf = await api.get(`/api/loantool/total/fine/${loan.id}`); setTotalFine(Number(tf.data || 0)); } catch (error_) { console.debug(error_); setTotalFine(0); }
      setStateToolMap({});
    } catch (err) {
      console.error('Error al devolver herramientas', err?.response ?? err);
      showReturnError(alert, err);
    } finally { setReturning(false); }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="max-w-6xl mx-auto big-page">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <div>
              <h2 style={{ margin: 0 }}>Devolver Pedido — Resumen</h2>
              <p style={{ margin: '4px 0 0', color: '#4b5563' }}>Revisa los datos del pedido y selecciona el estado de cada herramienta antes de confirmar la devolución.</p>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
              <BackButton to={`/admin/returns/client/${loan?.idUser?.id || ''}`} />
            </div>
          </div>
          {loading ? <LoadingSpinner message="Cargando resumen..." /> : (
            <div>
              {loan ? (
                <div style={{ marginTop: 12, display: 'flex', gap: 16 }}>
                  <section style={{ flex: '0 0 320px', background: '#fff', padding: 14, borderRadius: 8, border: '1px solid #e5e7eb' }}>
                    <h4 style={{ margin: 0, fontSize: 15 }}>Cliente</h4>
                    <div style={{ marginTop: 10 }}>
                      <div style={{ fontWeight: 800 }}>{getClientDisplayName(loan.idUser)}</div>
                      <div style={{ marginTop: 4, fontSize: 16, color: '#374151' }}>{loan.idUser?.username || loan.idUser?.email}</div>
                      {loan.idUser?.email && <div style={{ marginTop: 2, fontSize: 14, color: '#4b5563' }}>{loan.idUser.email}</div>}
                      {loan.idUser?.rut && <div style={{ marginTop: 6, fontSize: 16 }}>RUT: {loan.idUser.rut}</div>}
                    </div>
                  </section>

                  <section style={{ flex: 1, background: '#fff', padding: 14, borderRadius: 8, border: '1px solid #e5e7eb' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <div style={{ fontSize: 14, color: '#6b7280' }}>Resumen del pedido</div>
                        <div style={{ fontWeight: 700, fontSize: 18 }}>{`Pedido #${loan.id}`}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'flex-end' }}>
                          <Badge variant={statusToBadgeVariant(loan.status)} title={loan.status || ''} />
                          <div style={{ fontSize: 14 }}>{loan.status}</div>
                        </div>
                      </div>
                    </div>

                    <div style={{ marginTop: 12 }}>
                      <div>Fecha inicio: {formatDate(loan.initDate)}</div>
                      <div>Fecha devolución: {formatDate(loan.returnDate)}</div>
                    </div>

                    <h3 style={{ marginTop: 16 }}>Herramientas solicitadas</h3>
                    {items.length === 0 ? <p>No hay herramientas en este pedido.</p> : (
                      <div>
                        <ul className="card-list">
                          {pagedItems.map(it => (
                            <ReturnToolCard key={it.id} item={it} disabled={!isActive} onStateChange={(id, state) => setStateToolMap(prev => ({ ...prev, [id]: state }))} />
                          ))}
                        </ul>
                        <PaginationBar
                          page={safePage}
                          pageSize={pageSize}
                          total={total}
                          onPageChange={(p) => setPage(p)}
                          onPageSizeChange={(ps) => { setPageSize(ps); setPage(1); }}
                        />

                        <div style={{ marginTop: 12, display: 'flex', gap: 12, alignItems: 'center' }}>
                          {/* Botón de devolución (visible solo si el pedido no está finalizado) */}
                          {isActive && items.length > 0 && (
                            <button
                              className="primary-cta"
                              disabled={returning}
                              style={{ padding: '8px 16px' }}
                              onClick={handleReturn}
                            >
                              {returning ? 'Devolviendo...' : 'Devolver Herramientas'}
                            </button>
                          )}

                          {/* Botón de pagar deuda (visible si hay multa) */}
                          {totalFine > 0 && (
                            <button
                              className="primary-cta"
                              disabled={payingDebt}
                              style={{ padding: '8px 16px' }}
                              onClick={() => setShowDebtModal(true)}
                            >
                              {payingDebt ? 'Pagando deuda...' : `Pagar deuda (${new Intl.NumberFormat().format(totalFine)} )`}
                            </button>
                          )}

                          {/* Botón de pagar reparación (si hay herramientas con needRepair) */}
                          {items.some(it => it.needRepair) && (
                            <button className="primary-cta" style={{ padding: '8px 16px' }} onClick={() => setShowRepairModal(true)}>Pagar reparación</button>
                          )}
                        </div>
                      </div>
                    )}
                  </section>
                </div>
              ) : <p>Pedido no encontrado.</p>}
            </div>
          )}
          <RepairPaymentModal
            open={showRepairModal}
            onClose={() => setShowRepairModal(false)}
            loan={loan}
            initialItems={items.filter(it => it.needRepair)}
            onPaid={async () => {
              // refresh loan/items and totals after paying repairs
              if (!loan) return;
              await api.get(`/api/loantool/loan/${loan.id}`).then(r => setItems(r.data || []));
              await api.get(`/api/loan/${loan.id}`).then(r => setLoan(r.data)).catch(() => {});
              try { const tf = await api.get(`/api/loantool/total/fine/${loan.id}`); setTotalFine(Number(tf.data || 0)); } catch (error_) { console.debug(error_); setTotalFine(0); }
            }}
          />

          <DebtPaymentModal
            open={showDebtModal}
            onClose={() => setShowDebtModal(false)}
            loan={loan}
            totalFine={totalFine}
            onPaid={async () => {
              if (!loan) return;
              await api.get(`/api/loantool/loan/${loan.id}`).then(r => setItems(r.data || []));
              await api.get(`/api/loan/${loan.id}`).then(r => setLoan(r.data)).catch(() => {});
              try { const tf = await api.get(`/api/loantool/total/fine/${loan.id}`); setTotalFine(Number(tf.data || 0)); } catch (error_) { console.debug(error_); setTotalFine(0); }
            }}
          />
        </div>
      </main>
    </div>
  );
};

export default ReturnsLoanSummary;
