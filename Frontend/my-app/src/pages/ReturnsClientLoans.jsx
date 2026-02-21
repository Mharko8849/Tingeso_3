import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import PaginationBar from '../components/Common/PaginationBar';
import LoadingSpinner from '../components/Loading/LoadingSpinner';
import api from '../services/http-common';
import Badge from '../components/Badges/Badge';
import { statusToBadgeVariant } from '../components/Badges/statusToBadge';

const ReturnsClientLoans = () => {
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [client, setClient] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Estados de paginación
  const [page, setPage] = useState(1); // 1-indexed para el UI
  const [pageSize, setPageSize] = useState(8);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const clientId = window.location.pathname.split('/').pop();

  useEffect(() => {
    if (!clientId) return;
    fetchLoans();
  }, [clientId, page, pageSize]);

  const fetchLoans = async () => {
    setLoading(true);
    try {
      // Usar endpoint paginado del backend (page es 0-indexed en Spring)
      const response = await api.get(`/api/loan/user/${clientId}/paginated?page=${page - 1}&size=${pageSize}`);
      const data = response.data;
      
      setLoans(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
      
      // Obtener info del cliente del primer loan si hay resultados
      if (data.content && data.content.length > 0) {
        const firstLoan = data.content[0];
        setClient({
          name: firstLoan.clientName,
          lastName: '',
          username: firstLoan.username,
          email: firstLoan.clientEmail
        });
      }
    } catch (error) {
      console.error('Error fetching loans:', error);
      setLoans([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  const openLoan = (id) => {
    navigate(`/admin/returns/loan/${id}`);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setPage(1); // Reset a primera página
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="max-w-6xl mx-auto big-page">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h2 style={{ margin: 0 }}>Pedidos del cliente {client ? (client.name ? `${client.name} ${client.lastName || ''}` : (client.username || client.email)) : ''}</h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
              <BackButton to="/admin/returns" />
            </div>
          </div>
          
          {/* Controles de paginación superiores */}
          {!loading && totalElements > 0 && (
            <PaginationBar
              page={page}
              pageSize={pageSize}
              total={totalElements}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
              showPageSizeControls={true}
              showSummary={false}
            />
          )}
          
          {loading ? <LoadingSpinner message="Cargando pedidos..." /> : (
            loans.length === 0 && totalElements === 0 ? <p>El cliente no tiene pedidos.</p> : (
              <>
                <div style={{ marginTop: 12, maxHeight: 520, overflowY: 'auto', width: '100%' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 1fr 160px 120px', gap: 12, padding: '6px 8px', borderBottom: '1px solid #f1f5f9', marginBottom: 8, alignItems: 'center' }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Pedido #</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha inicio</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Fecha devolución</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Estado</div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: '#374151', textAlign: 'right' }}>Acciones</div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 10 }}>
                    {loans.map((l) => (
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
                        <div style={{ display: 'grid', gridTemplateColumns: '80px 1fr 1fr 160px 120px', alignItems: 'center', gap: 12, width: '100%' }}>
                          <div style={{ fontWeight: 800, fontSize: 16 }}>#{l.id}</div>
                          <div style={{ fontSize: 14, color: '#374151' }}>{l.initDate}</div>
                          <div style={{ fontSize: 14, color: '#374151' }}>{l.returnDate}</div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Badge variant={statusToBadgeVariant(l.status)} title={l.status || ''} />
                            <div style={{ fontSize: 14, color: '#374151' }}>{l.status}</div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            <button className="link" style={{ whiteSpace: 'nowrap' }} onClick={(ev) => { ev.stopPropagation(); openLoan(l.id); }}>Ver pedido</button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
                
                {/* Paginación inferior con resumen */}
                {!loading && loans.length > 0 && (
                  <PaginationBar
                    page={page}
                    pageSize={pageSize}
                    total={totalElements}
                    onPageChange={handlePageChange}
                    onPageSizeChange={handlePageSizeChange}
                    showPageSizeControls={false}
                    showSummary={true}
                  />
                )}
              </>
            )
          )}
        </div>
      </main>
    </div>
  );
};

export default ReturnsClientLoans;
