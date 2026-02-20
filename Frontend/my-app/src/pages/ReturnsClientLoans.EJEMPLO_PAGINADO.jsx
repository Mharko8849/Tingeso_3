import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import PaginationBar from '../components/Common/PaginationBar';
import api from '../services/http-common';
import Badge from '../components/Badges/Badge';
import { statusToBadgeVariant } from '../components/Badges/statusToBadge';

/**
 * VERSIÓN MEJORADA con DTOs y Paginación
 * Muestra los pedidos de un cliente ordenados por más recientes
 */
const ReturnsClientLoansPaginated = () => {
  const navigate = useNavigate();
  const { id: clientId } = useParams();
  
  const [loans, setLoans] = useState([]);
  const [client, setClient] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Estados de paginación
  const [page, setPage] = useState(0); // Spring usa 0-indexed
  const [pageSize, setPageSize] = useState(8);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (!clientId) return;
    fetchLoans();
  }, [clientId, page, pageSize]);

  const fetchLoans = async () => {
    setLoading(true);
    try {
      // Opción 1: Si tienes endpoint específico para user paginado
      // const res = await api.get(`/loan/user/${clientId}/paginated?page=${page}&size=${pageSize}`);
      
      // Opción 2: Traer todos los loans del usuario (sin paginación del backend)
      // y paginar en el frontend
      const res = await api.get(`/loan/user/${clientId}`);
      const allLoans = res.data || [];
      
      // Ordenar por ID descendente (más recientes primero)
      allLoans.sort((a, b) => b.id - a.id);
      
      // Paginar manualmente en el frontend
      const startIdx = page * pageSize;
      const endIdx = startIdx + pageSize;
      const paginatedLoans = allLoans.slice(startIdx, endIdx);
      
      setLoans(paginatedLoans);
      setTotalElements(allLoans.length);
      setTotalPages(Math.ceil(allLoans.length / pageSize));
      
      if (allLoans.length > 0) {
        setClient(allLoans[0].idUser);
      }
    } catch (error) {
      console.error('Error fetching loans:', error);
      setLoans([]);
    } finally {
      setLoading(false);
    }
  };

  const openLoan = (id) => {
    navigate(`/admin/returns/loan/${id}`);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage - 1); // PaginationBar usa 1-indexed, Spring usa 0-indexed
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setPage(0); // Reset a primera página
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main style={{ paddingTop: 30 }} className="px-6">
        <div className="max-w-6xl mx-auto big-page">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h2 style={{ margin: 0 }}>
                Pedidos del cliente{' '}
                {client
                  ? client.name
                    ? `${client.name} ${client.lastName || ''}`
                    : client.username || client.email
                  : ''}
              </h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
              <BackButton to="/admin/returns" />
            </div>
          </div>

          {/* Control de tamaño de página */}
          <PaginationBar
            page={page + 1}
            pageSize={pageSize}
            total={totalElements}
            onPageChange={handlePageChange}
            onPageSizeChange={handlePageSizeChange}
            showPageSizeControls={true}
            showSummary={false}
          />

          {loading ? (
            <p>Cargando pedidos...</p>
          ) : loans.length === 0 ? (
            <p>El cliente no tiene pedidos.</p>
          ) : (
            <div style={{ marginTop: 12, width: '100%' }}>
              {/* Cabecera de tabla */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '80px 1fr 1fr 160px 120px',
                  gap: 12,
                  padding: '6px 8px',
                  borderBottom: '1px solid #f1f5f9',
                  marginBottom: 8,
                  alignItems: 'center',
                }}
              >
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Pedido #</div>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha inicio</div>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha devolución</div>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Estado</div>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151', textAlign: 'right' }}>
                  Acciones
                </div>
              </div>

              {/* Lista de préstamos */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 10 }}>
                {loans.map((l) => (
                  <div
                    key={l.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => openLoan(l.id)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') openLoan(l.id);
                    }}
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
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: '80px 1fr 1fr 160px 120px',
                        alignItems: 'center',
                        gap: 12,
                        width: '100%',
                      }}
                    >
                      <div style={{ fontWeight: 800, fontSize: 16 }}>#{l.id}</div>
                      <div style={{ fontSize: 14, color: '#374151' }}>{l.initDate}</div>
                      <div style={{ fontSize: 14, color: '#374151' }}>{l.returnDate}</div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Badge variant={statusToBadgeVariant(l.status)} title={l.status || ''} />
                        <div style={{ fontSize: 14, color: '#374151' }}>{l.status}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <button
                          className="link"
                          style={{ whiteSpace: 'nowrap' }}
                          onClick={(ev) => {
                            ev.stopPropagation();
                            openLoan(l.id);
                          }}
                        >
                          Ver pedido
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Paginación inferior con resumen */}
          {!loading && loans.length > 0 && (
            <PaginationBar
              page={page + 1}
              pageSize={pageSize}
              total={totalElements}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
              showPageSizeControls={false}
              showSummary={true}
            />
          )}
        </div>
      </main>
    </div>
  );
};

export default ReturnsClientLoansPaginated;
