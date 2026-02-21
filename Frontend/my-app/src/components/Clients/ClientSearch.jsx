import React, { useEffect, useState } from 'react';
import api from '../../services/http-common';
import Badge from '../Badges/Badge';
import { statusToBadgeVariant } from '../Badges/statusToBadge';
import PaginationBar from '../Common/PaginationBar';

const ClientSearch = ({ selected, onSelect, reloadKey, hideHeader = false }) => {
  const [clients, setClients] = useState([]);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(5);

  useEffect(() => {
    let mounted = true;
    const fetchClients = async () => {
      try {
        const resp = await api.get('/api/user/clients');
        if (mounted) setClients(resp.data || []);
      } catch (e) {
        console.warn('Could not load clients', e);
        if (mounted) setClients([]);
      }
    };
    fetchClients();
    return () => { mounted = false };
  }, [reloadKey]);

  const filtered = clients.filter((c) => {
    if (!query) return true;
    const s = query.toLowerCase();
    return (String(c.username || '') + ' ' + String(c.name || '') + ' ' + String(c.lastName || '') + ' ' + String(c.email || '')).toLowerCase().includes(s);
  });

  // Reset to page 1 when search query changes
  useEffect(() => { setPage(1); }, [query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(Math.max(1, page), totalPages);
  const paged = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  return (
    <div>
      {!hideHeader && <>
        <label>Buscar cliente</label>
        <input placeholder="buscar por nombre/usuario/email" value={query} onChange={(e) => setQuery(e.target.value)} style={{ width: '100%', padding: 8, marginTop: 6, backgroundColor: '#ffffff', color: '#000000' }} />
      </>}
      {hideHeader && (
        <input placeholder="buscar por nombre/usuario/email" value={query} onChange={(e) => setQuery(e.target.value)} style={{ width: '100%', padding: 8, marginTop: 6, backgroundColor: '#ffffff', color: '#000000' }} />
      )}

      <div style={{ marginTop: 8, border: '1px solid #eee', borderRadius: 6, padding: 10, boxSizing: 'border-box' }}>
        {/* Column headers */}
        {filtered.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: '20% 30% 15% 15% 20%', gap: 12, padding: '6px 8px', borderBottom: '1px solid #f1f5f9', marginBottom: 8 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Usuario</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Email</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>RUT</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Pedidos</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#374151' }}>Estado</div>
          </div>
        )}

        {/* Client cards — only current page */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 10 }}>
          {paged.map((c) => {
            const isSel = selected?.id === c.id;
            return (
              <div
                key={c.id}
                onClick={() => onSelect && onSelect(isSel ? null : c)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { onSelect && onSelect(isSel ? null : c); } }}
                style={{
                  padding: 14,
                  borderRadius: 8,
                  border: '1px solid #e6e6e6',
                  background: isSel ? '#eef6ff' : '#fff',
                  cursor: 'pointer',
                  boxShadow: isSel ? '0 0 0 3px rgba(59,130,246,0.08)' : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 24,
                }}
              >
                <div style={{ display: 'grid', gridTemplateColumns: '20% 30% 15% 15% 20%', alignItems: 'center', gap: 12, width: '100%' }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 800, fontSize: 18, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {c.name ? `${c.name} ${c.lastName || ''}` : (c.username || c.email || `Cliente ${c.id}`)}
                    </div>
                    <div style={{ fontSize: 14, color: '#444', marginTop: 6, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{c.username || c.email}</div>
                  </div>

                  <div style={{ minWidth: 0, textAlign: 'left', paddingLeft: 8 }}>
                    <div style={{ fontSize: 15, color: '#374151', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{c.email}</div>
                  </div>

                  <div style={{ textAlign: 'left' }}>
                    <div style={{ fontSize: 14, color: '#374151', whiteSpace: 'nowrap' }}>{c.rut ? `RUT: ${c.rut}` : ''}</div>
                  </div>

                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 16, fontWeight: 700 }}>{c.loans != null ? c.loans : 0}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>pedidos</div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', gap: 8 }}>
                    <Badge
                      variant={statusToBadgeVariant(c.stateClient)}
                      title={c.stateClient || ''}
                    />
                    <div style={{ fontSize: 14, color: isSel ? '#065f46' : '#374151', whiteSpace: 'nowrap' }}>{c.stateClient || ''}</div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {filtered.length === 0 && <div style={{ padding: 12, color: '#666' }}>No se encontraron clientes</div>}

        {/* Pagination */}
        {filtered.length > pageSize && (
          <PaginationBar
            page={safePage}
            pageSize={pageSize}
            total={filtered.length}
            onPageChange={setPage}
            showPageSizeControls={false}
            showSummary={true}
          />
        )}
      </div>
    </div>
  );
};

export default ClientSearch;
