import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import PaginationBar from '../components/Common/PaginationBar';
import { HelpIcon } from '../components/Common/Tooltip';
import LoadingSpinner from '../components/Loading/LoadingSpinner';
import api from '../services/http-common';
import Badge from '../components/Badges/Badge';
import { statusToBadgeVariant } from '../components/Badges/statusToBadge';
import { ReportLoans } from '../components/Reports';
import { formatDate } from '../utils/validation';

const Loans = () => {
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState(''); // '', ACTIVO, FINALIZADO, PENDIENTE, ATRASADO

  // Pagination states
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(8);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchLoans = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size: pageSize };

      let endpoint = '/api/loan/paginated';
      if (status) {
        endpoint = '/api/loan/filter/paginated';
        params.state = status;
      }

      const res = await api.get(endpoint, { params });
      const data = res.data;

      // Extract pagination data from PageResponseDTO
      setLoans(Array.isArray(data.content) ? data.content : []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 1);
    } catch (e) {
      console.error('No se pudo cargar préstamos', e?.response ?? e);
      setError('No se pudo cargar la lista de pedidos.');
      setLoans([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLoans();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize, status]);

  const goBack = () => {
    navigate('/');
  };

  const openLoan = (id) => {
    // Navega al resumen de pedido de solo lectura
    navigate(`/loans/loan/${id}`);
  };

  const today = useMemo(() => (new Date()).toISOString().slice(0, 10), []);

  // Client-side search filter (already paginated data from backend)
  const filtered = useMemo(() => {
    if (!q) return loans;
    const term = q.toLowerCase();
    return loans.filter((l) => {
      const idStr = String(l.id || '');

      // Support both DTO format (clientName) and Entity format (idUser fallback)
      let clientNameStr = '';
      if (l.clientName || l.username || l.clientEmail) {
        clientNameStr = l.clientName ? l.clientName.trim() : (l.username || l.clientEmail || '');
      } else if (l.idUser) {
        const client = l.idUser;
        clientNameStr = client.name ? `${client.name} ${client.lastName || ''}` : (client.username || client.email || '');
      }

      return idStr.includes(term) || clientNameStr.toLowerCase().includes(term);
    });
  }, [loans, q]);


  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="max-w-6xl mx-auto big-page">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h2 style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                Pedidos — Todos
                <HelpIcon
                  content="Activo • Finalizado • Pendiente • Atrasado"
                  position="right"
                />
              </h2>
              <p style={{ margin: '4px 0 0', color: '#4b5563' }}>Listado general de pedidos del sistema.</p>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
              <BackButton onClick={goBack} />
            </div>
          </div>

          <div style={{ marginTop: 12, display: 'grid', gridTemplateColumns: '1fr auto auto auto', gap: 8, alignItems: 'center' }}>
            <input
              placeholder="Buscar por #pedido o cliente..."
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              onKeyDown={(e) => {
                // Redundant enter press as requested to ensure manual sync
                if (e.key === 'Enter') {
                  setPage(0);
                }
              }}
              style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', minWidth: 260, backgroundColor: '#ffffff', color: '#000000' }}
            />
            <select
              value={status}
              onChange={(e) => {
                const v = e.target.value;
                setStatus(v);
                setPage(0); // Reset to first page when filter changes
              }}
              style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db' }}
            >
              <option value="">Todos los estados</option>
              <option value="ACTIVO">Activo</option>
              <option value="FINALIZADO">Finalizado</option>
              <option value="PENDIENTE">Pendiente</option>
              <option value="ATRASADO">Atrasados</option>
            </select>
            <button
              onClick={fetchLoans}
              className="primary-cta"
              type="button"
              aria-label="Refrescar"
              title="Refrescar"
              style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M21 12a9 9 0 1 1-2.64-6.36" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M21 4v6h-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              Refrescar
            </button>
            <ReportLoans rows={filtered} />
          </div>

          {error && <div style={{ color: '#b91c1c', marginTop: 8 }}>{error}</div>}

          {loading ? <LoadingSpinner message="Cargando pedidos..." /> : (
            <>
              <PaginationBar
                page={page + 1}
                pageSize={pageSize}
                total={totalElements}
                onPageChange={(p) => setPage(p - 1)}
                onPageSizeChange={(ps) => { setPageSize(ps); setPage(0); }}
                showPageSizeControls={true}
                showSummary={false}
              />

              {filtered.length === 0 ? <p style={{ marginTop: 12 }}>No hay pedidos para mostrar.</p> : (
                <div style={{ marginTop: 12, maxHeight: 520, overflowY: 'auto', width: '100%' }}>
                  <div style={{ marginTop: 12, display: 'grid', gridTemplateColumns: '80px 1.2fr 1fr 1fr 1fr 160px 120px', gap: 12, padding: '6px 8px', borderBottom: '1px solid #f1f5f9', marginBottom: 8, alignItems: 'center' }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Pedido #</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Cliente</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha inicio</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha devolución</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha Actual</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Estado</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151', textAlign: 'right' }}>Acciones</div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 10 }}>
                    {filtered.map((l) => {
                      let clientName = '—';
                      if (l.clientName || l.username || l.clientEmail) {
                        clientName = l.clientName ? l.clientName.trim() : (l.username || l.clientEmail || '—');
                      } else if (l.idUser) {
                        const client = l.idUser;
                        clientName = client.name ? `${client.name} ${client.lastName || ''}` : (client.username || client.email || '—');
                      }

                      return (
                        <div
                          key={l.id}
                          role="button"
                          tabIndex={0}
                          onClick={() => openLoan(l.id)}
                          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') openLoan(l.id); }}
                          style={{
                            padding: 14,
                            borderRadius: 8,
                            border: '1px solid #e6e6e6',
                            background: '#fff',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 24,
                          }}
                        >
                          <div style={{ display: 'grid', gridTemplateColumns: '80px 1.2fr 1fr 1fr 1fr 160px 120px', alignItems: 'center', gap: 12, width: '100%' }}>
                            <div style={{ fontWeight: 800, fontSize: 16 }}>#{l.id}</div>
                            <div style={{ fontSize: 14, color: '#374151' }}>{clientName}</div>
                            <div style={{ fontSize: 14, color: '#374151' }}>{formatDate(l.initDate)}</div>
                            <div style={{ fontSize: 14, color: '#374151' }}>{formatDate(l.returnDate)}</div>
                            <div style={{ fontSize: 14, color: '#374151' }}>{formatDate(today)}</div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <Badge variant={statusToBadgeVariant(l.status)} title={l.status || ''} />
                              <div style={{ fontSize: 14, color: '#374151' }}>{l.status}</div>
                            </div>
                            <div style={{ textAlign: 'right' }}>
                              <button className="link" style={{ whiteSpace: 'nowrap' }} onClick={(ev) => { ev.stopPropagation(); openLoan(l.id); }}>Ver pedido</button>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              <PaginationBar
                page={page + 1}
                pageSize={pageSize}
                total={totalElements}
                onPageChange={(p) => setPage(p - 1)}
                onPageSizeChange={(ps) => { setPageSize(ps); setPage(0); }}
                showPageSizeControls={false}
                showSummary={true}
              />
            </>
          )}
        </div>
      </main>
    </div>
  );
};

export default Loans;
