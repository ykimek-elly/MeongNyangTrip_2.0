import React, { useEffect } from 'react';
import { RouterProvider } from 'react-router';
import { router } from './routes';
import { GlobalErrorBoundary } from './components/ErrorBoundary';
import './index.css';
import { useAppStore } from './store/useAppStore';

export default function App() {
  const fetchPlaces = useAppStore(state => state.fetchPlaces);

  useEffect(() => {
    fetchPlaces();
  }, [fetchPlaces]);

  return (
    <GlobalErrorBoundary>
      <RouterProvider router={router} />
    </GlobalErrorBoundary>
  );
}