/**
 * API Service - Handles all HTTP requests to the backend
 * 
 * Features:
 * - Axios instance with base URL and interceptors
 * - Automatic token attachment
 * - Error handling and response transformation
 * - Request/response logging in development
 */

import axios, { AxiosError } from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types';

// Base URL for API requests
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Create axios instance
const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach auth token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log requests in development
    if (import.meta.env.DEV) {
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
    }
    
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle errors
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // Handle 401 Unauthorized - redirect to login
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    
    // Log errors in development
    if (import.meta.env.DEV) {
      console.error('[API Error]', error.response?.data || error.message);
    }
    
    return Promise.reject(error);
  }
);

// ============================================================================
// Auth API
// ============================================================================

export const authApi = {
  login: (email: string, password: string) =>
    api.post('/v1/auth/login', { email, password }),

  register: (name: string, email: string, password: string) =>
    api.post('/v1/auth/register', { name, email, password }),

  getCurrentUser: () =>
    api.get('/v1/auth/me'),

  updateProfile: (name: string) =>
    api.put('/v1/auth/profile', { name }),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.put('/v1/auth/password', { currentPassword, newPassword }),
};

// ============================================================================
// Project API
// ============================================================================

export const projectApi = {
  getAll: () =>
    api.get('/v1/projects'),

  getById: (id: number) =>
    api.get(`/v1/projects/${id}`),

  create: (data: { name: string; description?: string }) =>
    api.post('/v1/projects', data),

  update: (id: number, data: { name: string; description?: string; active?: boolean }) =>
    api.put(`/v1/projects/${id}`, data),

  delete: (id: number) =>
    api.delete(`/v1/projects/${id}`),
};

// ============================================================================
// Endpoint API
// ============================================================================

export const endpointApi = {
  getByProject: (projectId: number) =>
    api.get('/v1/endpoints', { params: { projectId } }),

  getById: (id: number) =>
    api.get(`/v1/endpoints/${id}`),

  create: (data: {
    name: string;
    url: string;
    method: string;
    expectedStatusCode: number;
    checkIntervalSeconds: number;
    timeoutMs: number;
    projectId: number;
    headers?: string;
    requestBody?: string;
    maxLatencyMs?: number;
    credentialId?: number;
  }) =>
    api.post('/v1/endpoints', data),

  update: (id: number, data: {
    name?: string;
    url?: string;
    method?: string;
    expectedStatusCode?: number;
    checkIntervalSeconds?: number;
    timeoutMs?: number;
    headers?: string;
    requestBody?: string;
    maxLatencyMs?: number;
    credentialId?: number;
    enabled?: boolean;
  }) =>
    api.put(`/v1/endpoints/${id}`, data),

  delete: (id: number) =>
    api.delete(`/v1/endpoints/${id}`),

  toggle: (id: number, enabled: boolean) =>
    api.patch(`/v1/endpoints/${id}/toggle`, null, { params: { enabled } }),
};

// ============================================================================
// Credential API
// ============================================================================

export const credentialApi = {
  getByProject: (projectId: number) =>
    api.get('/v1/credentials', { params: { projectId } }),

  getById: (id: number) =>
    api.get(`/v1/credentials/${id}`),

  create: (data: {
    name: string;
    type: string;
    value: string;
    projectId: number;
    headerName?: string;
    username?: string;
    description?: string;
  }) =>
    api.post('/v1/credentials', data),

  update: (id: number, data: {
    name?: string;
    value?: string;
    headerName?: string;
    username?: string;
    description?: string;
  }) =>
    api.put(`/v1/credentials/${id}`, data),

  delete: (id: number) =>
    api.delete(`/v1/credentials/${id}`),
};

// ============================================================================
// Dashboard API
// ============================================================================

export const dashboardApi = {
  getOverview: () =>
    api.get('/v1/dashboard'),
};

// ============================================================================
// Alert API
// ============================================================================

export const alertApi = {
  getAll: (page = 0, size = 20) =>
    api.get('/v1/alerts', { params: { page, size } }),

  getUnacknowledged: () =>
    api.get('/v1/alerts/unacknowledged'),

  acknowledge: (id: number) =>
    api.post(`/v1/alerts/${id}/acknowledge`),

  acknowledgeAll: (endpointId: number) =>
    api.post('/v1/alerts/acknowledge-all', null, { params: { endpointId } }),
};

// ============================================================================
// Health API
// ============================================================================

export const healthApi = {
  check: () =>
    api.get('/health'),

  detailed: () =>
    api.get('/health/detailed'),
};

export default api;
