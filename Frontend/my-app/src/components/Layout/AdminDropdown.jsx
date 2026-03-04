import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import '../Tools/ToolDropdown.css';
import { cancelOrderDraft } from '../../services/orderDraft';
import ModalManageCategories from '../Categories/ModalManageCategories';
import ModalManageToolStates from '../Tools/ModalManageToolStates';

const AdminDropdown = ({ isAdminOrSuper }) => {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [showStatesModal, setShowStatesModal] = useState(false);

  const toggleMenu = () => setOpen(!open);
  const closeMenu = () => setOpen(false);

  const navigateTo = async (path) => {
    await cancelOrderDraft();
    navigate(path);
    closeMenu();
  };

  const handleOpenCategoryModal = () => {
    closeMenu();
    setShowCategoryModal(true);
  };

  const handleOpenStatesModal = () => {
    closeMenu();
    setShowStatesModal(true);
  };

  return (
    <>
      <button className="tool-btn" onClick={toggleMenu}>
        Administración
      </button>

      {open && (
        <div className="tool-overlay" >
      <button type="button" onClick={closeMenu} aria-label="Cerrar" style={{ position: 'absolute', inset: 0, background: 'transparent', border: 'none', cursor: 'default', zIndex: 0, padding: 0 }} />
          <div className="tool-content">
            <button className="close-btn" onClick={closeMenu}>✕</button>

            <div className="tool-grid">
              <div className="tool-section">
                <h4><strong>Pedidos</strong></h4>
                <button type="button" className="link-as-anchor" onClick={() => navigateTo('/admin/orders/create')}>Crear un pedido</button>
                <button type="button" className="link-as-anchor" onClick={() => navigateTo('/loans')}>Ver todos los pedidos</button>
                <button type="button" className="link-as-anchor" onClick={() => navigateTo('/admin/returns')}>Devolver y/o pagar un pedido</button>
              </div>
              <div className="tool-section">
                <h4><strong>Administración</strong></h4>
                {isAdminOrSuper && <button type="button" className="link-as-anchor" onClick={() => navigateTo('/employees')}>Empleados</button>}
                {isAdminOrSuper && <button type="button" className="link-as-anchor" onClick={() => navigateTo('/clients')}>Clientes</button>}
                <button type="button" className="link-as-anchor" onClick={() => navigateTo('/admin/kardex')}>Kardex</button>
              </div>
              <div className="tool-section">
                <h4><strong>Herramientas</strong></h4>
                <button type="button" className="link-as-anchor" onClick={() => navigateTo('/inventory')}>Ver inventario</button>
                {isAdminOrSuper && (
                  <>
                    <button type="button" className="link-as-anchor" onClick={handleOpenCategoryModal}>
                      Gestionar categorías
                    </button>
                    <button type="button" className="link-as-anchor" onClick={handleOpenStatesModal}>
                      Gestionar estados de herramientas
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      <ModalManageCategories
        open={showCategoryModal}
        onClose={() => setShowCategoryModal(false)}
      />

      <ModalManageToolStates
        open={showStatesModal}
        onClose={() => setShowStatesModal(false)}
      />
    </>
  );
};

AdminDropdown.propTypes = {
  isAdminOrSuper: PropTypes.bool,
};

export default AdminDropdown;
