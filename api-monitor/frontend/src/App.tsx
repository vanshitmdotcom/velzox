/**
 * API Monitor - Main Application Component
 * 
 * A developer-first API monitoring SaaS that:
 * - Periodically hits APIs (GET/POST)
 * - Supports authentication
 * - Measures availability, latency, and status
 * - Sends real-time alerts when something breaks
 */

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/layout';
import {
  LoginPage,
  RegisterPage,
  DashboardPage,
  ProjectsPage,
  EndpointsPage,
  CredentialsPage,
  AlertsPage,
} from './pages';

// Create React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30000, // 30 seconds
      retry: 1,
    },
  },
});

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected routes */}
            <Route element={<Layout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/projects" element={<ProjectsPage />} />
              <Route path="/endpoints" element={<EndpointsPage />} />
              <Route path="/credentials" element={<CredentialsPage />} />
              <Route path="/alerts" element={<AlertsPage />} />
              
              {/* Settings and Help placeholders */}
              <Route
                path="/settings"
                element={
                  <div className="p-6">
                    <h1 className="text-2xl font-bold text-gray-900 mb-4">Settings</h1>
                    <p className="text-gray-500">Settings page coming soon...</p>
                  </div>
                }
              />
              <Route
                path="/help"
                element={
                  <div className="p-6">
                    <h1 className="text-2xl font-bold text-gray-900 mb-4">Help & Support</h1>
                    <p className="text-gray-500">Help documentation coming soon...</p>
                  </div>
                }
              />
            </Route>

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
        
        {/* Global toast notifications */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#fff',
              color: '#333',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
              borderRadius: '8px',
              padding: '12px 16px',
            },
          }}
        />
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
