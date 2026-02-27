import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";
import 'bootstrap/dist/css/bootstrap.min.css';
import { BrowserRouter } from 'react-router-dom';

// Function to conditionally inject Microsoft Clarity
const injectClarity = () => {
  // Replace 'your-production-domain.com' with the actual production domain once you have it.
  // For now, we only block 'localhost' and typical Minikube/local IP patterns.
  const hostname = window.location.hostname;

  // Typical dev environments: localhost, 127.0.0.1, 192.168.x.x, 10.x.x.x
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';
  const isLocalNetwork = hostname.startsWith('192.168.') || hostname.startsWith('10.');

  if (!isLocalhost && !isLocalNetwork) {
    console.log("Production environment detected. Injecting Microsoft Clarity.");
    (function (c, l, a, r, i, t, y) {
      c[a] = c[a] || function () { (c[a].q = c[a].q || []).push(arguments) };
      t = l.createElement(r); t.async = 1; t.src = "https://www.clarity.ms/tag/" + i;
      y = l.getElementsByTagName(r)[0]; y.parentNode.insertBefore(t, y);
    })(window, document, "clarity", "script", "vn20pzhtpw");
  } else {
    console.log("Development environment detected. Microsoft Clarity tracking is disabled.");
  }
};

injectClarity();

ReactDOM.createRoot(document.getElementById('root')).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      checkLoginIframe: false,
      pkceMethod: 'S256'
    }}
  >
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </ReactKeycloakProvider>
)