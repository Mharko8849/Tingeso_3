import React from 'react';
import NavBar from '../Layout/NavBar';
import LoadingSpinner from './LoadingSpinner';

/**
 * PageLoader component
 * Full page loading state with NavBar.
 * Use this for entire page transitions or initial page loads.
 * 
 * @param {Object} props
 * @param {string} [props.message='Cargando página...'] - Loading message
 * @returns {JSX.Element}
 */
const PageLoader = ({ message = 'Cargando página...' }) => {
  return (
    <div className="bg-gray-50 min-h-screen">
      <NavBar />
      <main className="px-6" style={{ paddingTop: '90px', paddingBottom: '24px' }}>
        <div className="max-w-6xl mx-auto">
          <LoadingSpinner message={message} size="large" />
        </div>
      </main>
    </div>
  );
};

export default PageLoader;
