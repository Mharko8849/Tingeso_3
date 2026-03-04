import React, { Suspense, lazy } from 'react';
import PropTypes from 'prop-types';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AlertProvider } from './components/Alerts/AlertContext';
import { useKeycloak } from '@react-keycloak/web';
import { getUser } from './services/auth';

// Lazy load pages
const Home = lazy(() => import('./pages/Home'));
const InventoryPage = lazy(() => import('./pages/InventoryPage'));
const ToolDetail = lazy(() => import('./pages/ToolDetail'));
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Orders = lazy(() => import('./pages/Orders'));
const OrdersIndex = lazy(() => import('./pages/OrdersIndex'));
const Loans = lazy(() => import('./pages/Loans'));
const OrdersCreateClient = lazy(() => import('./pages/OrdersCreateClient'));
const OrdersCreateTools = lazy(() => import('./pages/OrdersCreateTools'));
const EmployeesAdministration = lazy(() => import('./pages/EmployeesAdministration'));
const ClientsAdministration = lazy(() => import('./pages/ClientsAdministration'));
const ResumeLoan = lazy(() => import('./pages/ResumeLoan'));
const ReturnsClients = lazy(() => import('./pages/ReturnsClients'));
const ReturnsClientLoans = lazy(() => import('./pages/ReturnsClientLoans'));
const ReturnsLoanSummary = lazy(() => import('./pages/ReturnsLoanSummary'));
const LoanSummaryReadOnly = lazy(() => import('./pages/LoanSummaryReadOnly'));
const KardexPage = lazy(() => import('./pages/KardexPage'));
const UsersDetails = lazy(() => import('./pages/UsersDetails'));

// lightweight placeholder for tools admin (not implemented yet)
const ToolsAdmin = () => <h2>Administración de herramientas (pendiente)</h2>;

const PrivateRoute = ({ element, rolesAllowed }) => {
  const { keycloak, initialized } = useKeycloak();
  const user = getUser();
  const effectiveLoggedIn = (initialized && keycloak.authenticated) || !!user;
  let roles = [];
  if (initialized && keycloak.authenticated && keycloak.tokenParsed?.realm_access) {
    roles = keycloak.tokenParsed.realm_access.roles || [];
  } else if (user?.realm_access && Array.isArray(user.realm_access.roles)) {
    roles = user.realm_access.roles;
  }
  roles = roles.map((r) => String(r).toUpperCase());

  if (!effectiveLoggedIn) {
    return <Navigate to="/login" replace />;
  }
  if (rolesAllowed && !rolesAllowed.some(r => roles.includes(r))) {
    return <h2>No tienes permiso para ver esta página</h2>;
  }
  return element;
};

PrivateRoute.propTypes = {
  element: PropTypes.node.isRequired,
  rolesAllowed: PropTypes.arrayOf(PropTypes.string),
};

const App = () => {
  // Force re-render on route change to ensure auth state is fresh
  useLocation();

  return (
    <AlertProvider>
      <Suspense fallback={<div style={{ padding: '2rem', textAlign: 'center' }}>Cargando...</div>}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        
        <Route path="/product/:id" element={<ToolDetailWrapper />} />
        
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/inventory/:category" element={<InventoryPageWrapper />} />
        
        {/* User Settings Route - Accessible by any logged in user */}
        <Route path="/profile" element={<PrivateRoute element={<UsersDetails />} rolesAllowed={['CLIENT', 'EMPLOYEE', 'ADMIN', 'SUPERADMIN']} />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<PrivateRoute element={<Orders />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/orders" element={<PrivateRoute element={<OrdersIndex />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/loans" element={<PrivateRoute element={<Loans />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/orders/create" element={<PrivateRoute element={<OrdersCreateClient />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/orders/create/tools" element={<PrivateRoute element={<OrdersCreateTools />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/returns" element={<PrivateRoute element={<ReturnsClients />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/returns/client/:id" element={<PrivateRoute element={<ReturnsClientLoans />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/returns/loan/:id" element={<PrivateRoute element={<ReturnsLoanSummary />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/loans/loan/:id" element={<PrivateRoute element={<LoanSummaryReadOnly />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/orders/resume" element={<PrivateRoute element={<ResumeLoan />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/tools" element={<PrivateRoute element={<ToolsAdmin />} rolesAllowed={['ADMIN','SUPERADMIN']} />} />
        <Route path="/admin/kardex" element={<PrivateRoute element={<KardexPage />} rolesAllowed={['EMPLOYEE','ADMIN','SUPERADMIN']} />} />
        <Route path="/employees" element={<PrivateRoute element={<EmployeesAdministration />} rolesAllowed={['ADMIN','SUPERADMIN']} />} />
        <Route path="/clients" element={<PrivateRoute element={<ClientsAdministration />} rolesAllowed={['ADMIN','SUPERADMIN']} />} />
        </Routes>
      </Suspense>
    </AlertProvider>
  );
};

// Wrapper components to handle params
import { useParams } from 'react-router-dom';

const ToolDetailWrapper = () => {
  const { id } = useParams();
  return <ToolDetail id={decodeURIComponent(id)} />;
};

const InventoryPageWrapper = () => {
  const { category } = useParams();
  return <InventoryPage category={category ? decodeURIComponent(category) : null} />;
};

export default App;
