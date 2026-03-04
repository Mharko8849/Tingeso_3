import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App.jsx';
import { ReactKeycloakProvider } from '@react-keycloak/web';
import keycloak from './services/keycloak';
import 'bootstrap/dist/css/bootstrap.min.css';
import { BrowserRouter } from 'react-router-dom';

// Function to conditionally inject Microsoft Clarity
const injectClarity = () => {
  // Replace 'your-production-domain.com' with the actual production domain once you have it.
  // For now, we only block 'localhost' and typical Minikube/local IP patterns.
  const hostname = globalThis.location.hostname;

  // Typical dev environments: localhost, 127.0.0.1, 192.168.x.x, 10.x.x.x
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';
  const isLocalNetwork = hostname.startsWith('192.168.') || hostname.startsWith('10.');

  if (!isLocalhost && !isLocalNetwork) {
    // eslint-disable-next-line no-console
    console.log('Production environment detected. Injecting Microsoft Clarity.');
    (function (c, l, a, r, i) {
      c[a] = c[a] || function () { c[a].q = c[a].q || []; c[a].q.push(arguments); };
      const tag = l.createElement(r); tag.async = 1; tag.src = `https://www.clarity.ms/tag/${  i}`;
      const first = l.getElementsByTagName(r)[0]; first.parentNode.insertBefore(tag, first);
    })(globalThis, document, 'clarity', 'script', 'vn20pzhtpw');
  } else {
    // eslint-disable-next-line no-console
    console.log('Development environment detected. Microsoft Clarity tracking is disabled.');
  }
};

injectClarity();

ReactDOM.createRoot(document.getElementById('root')).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${globalThis.location.origin  }/silent-check-sso.html`,
      checkLoginIframe: false,
      pkceMethod: 'S256',
    }}
  >
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </ReactKeycloakProvider>,
);