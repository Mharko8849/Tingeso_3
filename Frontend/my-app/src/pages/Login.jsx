import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
// import { useKeycloak } from '@react-keycloak/web';
import api from '../services/http-common';
import NavBar from '../components/Layout/NavBar';
import { useAlert } from '../components/Alerts/useAlert';
import './forms.css';

const Login = () => {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState(null);
  const { show } = useAlert();
  // const { keycloak } = useKeycloak();

  const submit = async (e) => {
    e.preventDefault();
    setMsg(null);
    // Use backend password-grant flow: submit credentials to our backend which
    // will request tokens from Keycloak and return them. We store tokens
    // locally and treat the user as logged in.
    try {
      setLoading(true);
      const body = { username: identifier, password };
      // Use axios instance which points to backend (VITE env vars or defaults)
      const resp = await api.post('/api/auth/login', body);
      const data = resp.data;

      // Extract user name for welcome message
      const userName = data?.user?.name || data?.name || identifier;

      // store tokens locally (the backend returns a structure { token: { access_token, refresh_token }, user: {...} })
      // Support both old and new shapes for compatibility.
      if (data) {
        // shape: { access_token, refresh_token }
        if (data.access_token) {
          localStorage.setItem('access_token', data.access_token);
          if (data.refresh_token) localStorage.setItem('refresh_token', data.refresh_token);
        }

        // shape: { token: { access_token, refresh_token } }
        else if (data.token && data.token.access_token) {
          localStorage.setItem('access_token', data.token.access_token);
          if (data.token.refresh_token) localStorage.setItem('refresh_token', data.token.refresh_token);
          // also keep alias used elsewhere
          localStorage.setItem('app_token', data.token.access_token);
        }

        // Store user data if available
        if (data.user) {
          localStorage.setItem('user', JSON.stringify(data.user));
        }
      }

      // Show success alert with user's name
      show({
        message: `¡Bienvenido ${userName}! Sesión iniciada correctamente`,
        severity: 'success',
        autoHideMs: 3500
      });

      // redirect to home using React Router
      setTimeout(() => {
        navigate('/');
      }, 500); // Small delay to show the alert before redirecting
    } catch (err) {
      console.error(err);
      if (err.response && err.response.status === 401) {
        setMsg('Credenciales incorrectas. Por favor verifica tu usuario y contraseña.');
      } else if (err.response && err.response.status === 404) {
        setMsg('El usuario ingresado no se encuentra registrado.\nCrea tu cuenta ahora mismo presionando el botón:\n"Crear cuenta".');
      } else {
        setMsg('Ocurrió un error al iniciar sesión. Inténtalo de nuevo más tarde.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6">
        <div className="form-card max-w-md mx-auto bg-white p-8 rounded shadow">
          <h2 className="form-title">Iniciar sesión</h2>
          <p className="form-sub">Ingresa con tu usuario o email</p>

          <form onSubmit={submit} className="form-grid">
            <div className="input-group">
              <label>Usuario o Email</label>
              <div className="input-with-icon">
                <span className="icon" aria-hidden>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M3 8l9 6 9-6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /><path d="M21 8v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
                </span>
                <input id="identifier" name="identifier" value={identifier} onChange={(e) => setIdentifier(e.target.value)} placeholder="usuario o email" required />
              </div>
            </div>

            <div className="input-group">
              <label>Contraseña</label>
              <div className="input-with-icon">
                <span className="icon" aria-hidden>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" /><path d="M7 11V8a5 5 0 0 1 10 0v3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" /></svg>
                </span>
                <input id="password" name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="tu contraseña" required />
              </div>
            </div>

            <div className="actions">
              <button className="primary-cta" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>{loading ? 'Entrando...' : 'Ingresar'}</button>
              <button type="button" className="link" style={{ width: '100%', justifyContent: 'center' }} onClick={() => navigate('/register')}>Crear cuenta</button>
            </div>
          </form>

          {msg && <div className="form-message" style={{ whiteSpace: 'pre-wrap' }}>{msg}</div>}
        </div>
      </main>
    </div>
  );
};

export default Login;
