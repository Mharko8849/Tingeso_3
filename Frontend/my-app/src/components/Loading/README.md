# Componentes de Carga (Loading)

Componentes reutilizables para mostrar estados de carga en la aplicación ToolRent.

## Componentes Disponibles

### 1. LoadingSpinner
Spinner animado con mensaje personalizable.

**Uso:**
```jsx
import LoadingSpinner from '../components/Loading/LoadingSpinner';

// Uso básico
<LoadingSpinner />

// Con mensaje personalizado
<LoadingSpinner message="Cargando productos..." />

// Tamaño pequeño
<LoadingSpinner size="small" message="Procesando..." />

// Pantalla completa con overlay
<LoadingSpinner fullScreen message="Guardando cambios..." />
```

**Props:**
- `size` (string): Tamaño del spinner - 'small', 'medium', 'large'. Default: 'medium'
- `message` (string): Mensaje a mostrar. Default: 'Cargando...'
- `fullScreen` (boolean): Si es true, muestra overlay en pantalla completa. Default: false
- `className` (string): Clases CSS adicionales

### 2. PageLoader
Loading de página completa con NavBar incluido.

**Uso:**
```jsx
import PageLoader from '../components/Loading/PageLoader';

// En renders condicionales
if (loading) return <PageLoader />;

// Con mensaje personalizado
if (loading) return <PageLoader message="Cargando datos del usuario..." />;
```

**Props:**
- `message` (string): Mensaje a mostrar. Default: 'Cargando página...'

## Ejemplos de Implementación

### En una página con datos asíncronos:
```jsx
const MyPage = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await api.get('/endpoint');
      setData(res.data);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        {loading ? (
          <LoadingSpinner message="Cargando datos..." />
        ) : (
          <div>
            {/* Contenido de la página */}
          </div>
        )}
      </main>
    </div>
  );
};
```

### Para página completa:
```jsx
const MyPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  if (loading) return <PageLoader message="Cargando información..." />;

  return (
    <div>
      {/* Página renderizada */}
    </div>
  );
};
```

## Estilos

Los componentes siguen el sistema de diseño de la aplicación:
- **Colores**: #60a5fa, #4f46e5, #3b82f6 (azules del tema)
- **Animaciones**: Rotación fluida con cubic-bezier
- **Accesibilidad**: Respeta `prefers-reduced-motion` para usuarios sensibles al movimiento

## Páginas Actualizadas

El componente LoadingSpinner ya está integrado en:
- ✅ KardexPage
- ✅ Loans
- ✅ InventoryPage
- ✅ ClientsAdmin
- ✅ EmployeesAdministration
- ✅ ToolDetail
- ✅ Orders
- ✅ OrdersCreateTools
- ✅ LoanSummaryReadOnly
- ✅ ReturnsLoanSummary
- ✅ ReturnsClientLoans
- ✅ UsersDetails
- ✅ Home

## Importación simplificada

Puedes importar ambos componentes desde el index:
```jsx
import { LoadingSpinner, PageLoader } from '../components/Loading';
```
