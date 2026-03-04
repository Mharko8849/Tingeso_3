import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import PaginationBar from '../components/Common/PaginationBar';
import { LoanListItem, LoanListHeader } from '../components/Common/LoanListItem';
import LoadingSpinner from '../components/Loading/LoadingSpinner';
import api from '../services/http-common';

const ReturnsClientLoans = () => {
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [client, setClient] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Estados de paginación
  const [page, setPage] = useState(1); // 1-indexed para el UI
  const [pageSize, setPageSize] = useState(8);
  const [totalElements, setTotalElements] = useState(0);

  const clientId = globalThis.location.pathname.split('/').pop();

  useEffect(() => {
    if (!clientId) return;
    fetchLoans();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId, page, pageSize]);

  const fetchLoans = async () => {
    setLoading(true);
    try {
      // Usar endpoint paginado del backend (page es 0-indexed en Spring)
      const response = await api.get(`/api/loan/user/${clientId}/paginated?page=${page - 1}&size=${pageSize}`);
      const data = response.data;
      
      setLoans(data.content || []);
      setTotalElements(data.totalElements || 0);
      
      // Obtener info del cliente del primer loan si hay resultados
      if (data.content?.length > 0) {
        const firstLoan = data.content[0];
        setClient({
          name: firstLoan.clientName,
          lastName: '',
          username: firstLoan.username,
          email: firstLoan.clientEmail,
        });
      }
    } catch (error) {
      console.error('Error fetching loans:', error);
      setLoans([]);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  const openLoan = (id) => {
    navigate(`/admin/returns/loan/${id}`);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
    globalThis.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setPage(1); // Reset a primera página
  };

  const clientDisplayName = (() => { if (!client) { return ''; } if (client.name) { return `${client.name} ${client.lastName || ''}`; } return client.username || client.email || ''; })();

    return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="max-w-6xl mx-auto big-page">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h2 style={{ margin: 0 }}>Pedidos del cliente {clientDisplayName}</h2>
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
          
          {(() => { if (loading) { return <LoadingSpinner message="Cargando pedidos..." />; } if (loans.length === 0 && totalElements === 0) { return <p>El cliente no tiene pedidos.</p>; } return (
              <>
                <div style={{ marginTop: 12, maxHeight: 520, overflowY: 'auto', width: '100%' }}>
                  <LoanListHeader
                    gridTemplate="80px 1fr 1fr 160px 120px"
                    headers={['Pedido #', 'Fecha inicio', 'Fecha devolución', 'Estado', 'Acciones']}
                  />

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 10 }}>
                    {loans.map((l) => (
                      <LoanListItem
                        key={l.id}
                        loan={l}
                        gridTemplate="80px 1fr 1fr 160px 120px"
                        columns={[
                          { key: 'initDate' },
                          { key: 'returnDate' },
                        ]}
                        onClick={() => openLoan(l.id)}
                      />
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
          ; })()}
        </div>
      </main>
    </div>
  );
};

export default ReturnsClientLoans;
