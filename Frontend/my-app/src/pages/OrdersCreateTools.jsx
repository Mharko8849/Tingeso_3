import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/Layout/NavBar';
import BackButton from '../components/Common/BackButton';
import CategoryListing from '../components/Categories/CategoryListing';
import OrderItemsDrawer from '../components/Orders/OrderItemsDrawer';
import api from '../services/http-common';
import TransitionAlert from '../components/Alerts/TransitionAlert';

const OrdersCreateTools = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ minPrice: 0, maxPrice: 500000 });
  const [tools, setTools] = useState([]);
  const [loadingTools, setLoadingTools] = useState(false);

  const [selectedClient, setSelectedClient] = useState(null);
  const [items, setItems] = useState([]);
  const [creating, setCreating] = useState(false);
  const [alert, setAlert] = useState(null);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem('order_selected_client');
      if (raw) setSelectedClient(JSON.parse(raw));
      // restore any locally selected items
      const its = sessionStorage.getItem('order_items');
      if (its) {
        try { setItems(JSON.parse(its)); } catch (e) { /* ignore parse errors */ }
      }
    } catch (e) { console.warn('could not read selected client', e); }
    fetchTools(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchTools = async (f) => {
    setLoadingTools(true);
    try {
      const qs = {};
      if (f?.minPrice) qs.minPrice = f.minPrice;
      if (f?.maxPrice) qs.maxPrice = f.maxPrice;
      if (f?.search) qs.search = f.search;
      if (f?.asc) qs.asc = true;
      if (f?.desc) qs.desc = true;
      if (f?.recent) qs.recent = true;
      
      const resp = await api.get('/inventory/filter', { params: qs });
      let inv = resp.data || [];

      // Fallback to tools endpoint if inventory returns empty
      if (!Array.isArray(inv) || inv.length === 0) {
        const toolsResp = await api.get('/tool/');
        const tools = toolsResp.data || [];
        inv = tools.map(tool => ({
          idTool: tool,
          toolState: { state: 'DISPONIBLE' },
          stockTool: 0
        }));
      }

      // Group inventory entries by tool id and compute available stock
      const map = new Map();
      const inventoryArray = Array.isArray(inv) ? inv : [];
      inventoryArray.forEach((entry) => {
        const t = entry.idTool || {};
        const tid = t.id;
        if (!map.has(tid)) {
          map.set(tid, {
            id: tid,
            name: t.toolName || t.name || '—',
            price: t.priceRent || t.price || 0,
            category: (typeof t.category === 'string' ? t.category : t.category?.name) || '',
            image: t.imageUrl ? `/images/${t.imageUrl}` : '/images/NoImage.png',
            stock: 0,
          });
        }
        const item = map.get(tid);
        // sum only available stock entries
        const stateName = typeof entry.toolState === 'string' ? entry.toolState : entry.toolState?.state;
        if (stateName === 'DISPONIBLE') {
          item.stock = (item.stock || 0) + (entry.stockTool || 0);
        }
      });

      setTools(Array.from(map.values()));
    } catch (e) {
      console.warn('fetchTools failed', e);
      setTools([]);
    } finally { setLoadingTools(false); }
  };

  const addTool = (t) => {
    if (!t || !t.id) return;
    // Business rule: only one unit per tool per client/order allowed.
    const exists = items.find(p => p.id === t.id);
    if (exists) {
      setAlert({ severity: 'error', message: 'El cliente ya tiene esta herramienta en el pedido. Sólo 1 unidad por herramienta.' });
      return;
    }
    // Validate with backend that the client doesn't already have this tool in another active loan
    (async () => {
      try {
        if (!selectedClient || !selectedClient.id) {
          setAlert({ severity: 'error', message: 'No hay cliente seleccionado. Vuelve atrás.' });
          return;
        }

        // Check if tool has available stock
        const stockResp = await api.get(`/inventory/check-stock/${t.id}`);
        const hasStock = stockResp.data === true;
        if (!hasStock) {
          setAlert({ severity: 'error', message: 'No hay stock disponible para realizar el préstamo.' });
          return;
        }

        const resp = await api.get(`/api/loantool/validate/${selectedClient.id}/${t.id}`);
        const already = resp.data === true;
        if (already) {
          setAlert({ severity: 'error', message: 'El cliente ya tiene esta herramienta en otro pedido activo.' });
          return;
        }
      } catch (e) {
        console.error('Validation failed:', e?.response?.data || e.message || e);
        setAlert({ severity: 'error', message: 'Error al validar la herramienta. Por favor intente nuevamente.' });
        return;
      }

      // include price if available (from the tool object or from current tools list)
      const toolObj = tools.find(x => x.id === t.id);
      const price = (t.price !== undefined && t.price !== null) ? t.price : (toolObj ? toolObj.price : 0);
      const image = toolObj ? toolObj.image : (t.image || '/NoImage.png');
      const newItem = { id: t.id, name: t.name, qty: 1, stock: t.stock, image, price };
      const next = [newItem, ...items];
      setItems(next);
      try { sessionStorage.setItem('order_items', JSON.stringify(next)); } catch (e) { /* ignore */ }
      // show immediate success feedback to the user
      setAlert({ severity: 'success', message: 'Herramienta agregada al pedido.' });
    })();
  };

  // Quantity limited to 1 per business rules.
  const changeQty = (id, qty) => setItems(s => s.map(i => i.id === id ? { ...i, qty: 1 } : i));
  const removeItem = async (id) => {
    // Option B: LoanXTools are local until confirm; removing item just updates local state
    const next = items.filter(i => i.id !== id);
    setItems(next);
    try { sessionStorage.setItem('order_items', JSON.stringify(next)); } catch (e) { /* ignore */ }
  };

  const goBackSelectClient = async () => {
    // Clear local and session draft state (no loan to delete since it hasn't been created yet)
    setItems([]);
    setSelectedClient(null);
    try {
      sessionStorage.removeItem('order_selected_client');
      sessionStorage.removeItem('order_items');
      sessionStorage.removeItem('order_loan_id');
      sessionStorage.removeItem('order_resume');
      sessionStorage.removeItem('order_init_date');
      sessionStorage.removeItem('order_return_date');
    } catch (e) {}

    navigate('/admin/orders/create');
  };

  const createOrder = async () => {
    // Prepare for resume/summary page
    if (!selectedClient) { setAlert({ severity: 'error', message: 'No hay cliente seleccionado. Vuelve atrás.' }); return; }
    if (!items || items.length === 0) { setAlert({ severity: 'error', message: 'Agrega al menos una herramienta' }); return; }

    for (const it of items) {
      if (it.stock !== undefined && it.qty > it.stock) { setAlert({ severity: 'error', message: `Cantidad solicitada mayor al stock para ${it.name}` }); return; }
    }

    // Save resume data to sessionStorage (no loanId yet - will be created in ResumeLoan)
    const resume = { client: selectedClient, items };
    try {
      sessionStorage.setItem('order_resume', JSON.stringify(resume));
      // navigate to resume page
      navigate('/admin/orders/resume');
    } catch (e) {
      console.warn('Could not save resume data', e);
      setAlert({ severity: 'error', message: 'No se pudo preparar el resumen del pedido' });
    }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      {alert && <TransitionAlert alert={alert} onClose={() => setAlert(null)} />}
      <main className="px-6">
        <div className="max-w-6xl mx-auto">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ margin: 0 }}>Crear Pedido — Herramientas</h2>
              </div>
              <div style={{ marginTop: 6 }}>{selectedClient ? `Cliente: ${selectedClient.username} — ${selectedClient.name || ''}` : 'Cliente no seleccionado'}</div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
              <BackButton onClick={goBackSelectClient} />
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            {loadingTools ? <div>Cargando herramientas...</div> : (
              <CategoryListing tools={tools} onApplyFilters={fetchTools} initialFilters={filters} toolCardProps={{ showAdd: true, onAdd: addTool, addDisabled: (t) => (t.stock <= 0) || items.some(it => it.id === t.id) }} />
            )}
          </div>

          {/* alert moved to top (below NavBar) */}
        </div>
      </main>

      {/* Drawer with horizontal list of selected items */}
      <OrderItemsDrawer items={items} onChangeQty={changeQty} onRemove={removeItem} onCreate={createOrder} onCancel={async () => {
        // Clear state and return to create page (no loan to delete)
        setItems([]);
        try { 
          sessionStorage.removeItem('order_selected_client'); 
          sessionStorage.removeItem('order_items'); 
          sessionStorage.removeItem('order_loan_id');
          sessionStorage.removeItem('order_init_date');
          sessionStorage.removeItem('order_return_date');
        } catch (e) {}
        navigate('/admin/orders/create');
      }} creating={creating} />
    </div>
  );
};

export default OrdersCreateTools;
