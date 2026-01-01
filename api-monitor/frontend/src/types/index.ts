/**
 * API Monitor - TypeScript Type Definitions
 * 
 * Contains all shared types for the frontend application.
 */

// ============================================================================
// User & Authentication
// ============================================================================

export interface User {
  id: number;
  name: string;
  email: string;
  plan: 'FREE' | 'STARTER' | 'PRO';
  emailVerified: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

// ============================================================================
// Project
// ============================================================================

export interface Project {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  endpointCount: number;
  credentialCount: number;
  createdAt: string;
  updatedAt: string;
  stats?: ProjectStats;
}

export interface ProjectListItem {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  endpointCount: number;
  upCount: number;
  downCount: number;
  createdAt: string;
}

export interface ProjectStats {
  totalEndpoints: number;
  upEndpoints: number;
  downEndpoints: number;
  degradedEndpoints: number;
  overallUptime: number;
  avgLatencyMs: number;
  openIncidents: number;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string;
  active?: boolean;
}

// ============================================================================
// Endpoint
// ============================================================================

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD';
export type EndpointStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';

export interface Endpoint {
  id: number;
  name: string;
  url: string;
  method: HttpMethod;
  headers?: string;
  requestBody?: string;
  expectedStatusCode: number;
  checkIntervalSeconds: number;
  timeoutMs: number;
  maxLatencyMs?: number;
  credentialId?: number;
  credentialName?: string;
  projectId: number;
  projectName: string;
  status: EndpointStatus;
  enabled: boolean;
  lastCheckAt?: string;
  nextCheckAt?: string;
  consecutiveFailures: number;
  createdAt: string;
  updatedAt: string;
  stats?: EndpointStats;
}

export interface EndpointListItem {
  id: number;
  name: string;
  url: string;
  method: HttpMethod;
  status: EndpointStatus;
  enabled: boolean;
  lastCheckAt?: string;
  uptimePercentage?: number;
  avgLatencyMs?: number;
  lastFailureAt?: string;
}

export interface EndpointStats {
  uptimePercentage: number;
  avgLatencyMs: number;
  totalChecks: number;
  successfulChecks: number;
  failedChecks: number;
  lastFailureAt?: string;
  currentIncidentId?: number;
}

export interface CreateEndpointRequest {
  name: string;
  url: string;
  method: HttpMethod;
  headers?: string;
  requestBody?: string;
  expectedStatusCode: number;
  checkIntervalSeconds: number;
  timeoutMs: number;
  maxLatencyMs?: number;
  credentialId?: number;
  projectId: number;
}

export interface UpdateEndpointRequest {
  name?: string;
  url?: string;
  method?: HttpMethod;
  headers?: string;
  requestBody?: string;
  expectedStatusCode?: number;
  checkIntervalSeconds?: number;
  timeoutMs?: number;
  maxLatencyMs?: number;
  credentialId?: number;
  enabled?: boolean;
}

// ============================================================================
// Credential
// ============================================================================

export type CredentialType = 'BEARER_TOKEN' | 'API_KEY' | 'BASIC_AUTH';

export interface Credential {
  id: number;
  name: string;
  type: CredentialType;
  maskedValue: string;
  headerName?: string;
  maskedUsername?: string;
  description?: string;
  projectId: number;
  inUse: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCredentialRequest {
  name: string;
  type: CredentialType;
  value: string;
  headerName?: string;
  username?: string;
  description?: string;
  projectId: number;
}

export interface UpdateCredentialRequest {
  name?: string;
  value?: string;
  headerName?: string;
  username?: string;
  description?: string;
}

// ============================================================================
// Alert
// ============================================================================

export type AlertType = 'ENDPOINT_DOWN' | 'ENDPOINT_RECOVERED' | 'AUTH_FAILURE' | 
                        'TIMEOUT' | 'SSL_ERROR' | 'LATENCY_BREACH' | 'CONNECTION_ERROR';
export type AlertSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
export type AlertChannel = 'EMAIL' | 'SLACK' | 'WEBHOOK';

export interface Alert {
  id: number;
  endpointId: number;
  endpointName: string;
  endpointUrl: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  message: string;
  channel: AlertChannel;
  delivered: boolean;
  deliveryError?: string;
  acknowledged: boolean;
  acknowledgedAt?: string;
  incidentId?: number;
  createdAt: string;
}

export interface AlertListItem {
  id: number;
  endpointName: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  acknowledged: boolean;
  createdAt: string;
}

// ============================================================================
// Dashboard
// ============================================================================

export interface DashboardOverview {
  totalEndpoints: number;
  upEndpoints: number;
  downEndpoints: number;
  degradedEndpoints: number;
  unknownEndpoints: number;
  overallUptimePercentage: number;
  avgLatencyMs: number;
  openIncidents: number;
  resolvedIncidentsToday: number;
  unacknowledgedAlerts: number;
  planUsage: PlanUsage;
  endpointStatuses: DashboardEndpointStatus[];
  recentAlerts: AlertListItem[];
  openIncidentsList: IncidentSummary[];
}

export interface PlanUsage {
  planName: string;
  maxEndpoints: number;
  usedEndpoints: number;
  minCheckIntervalSeconds: number;
  historyDays: number;
  slackEnabled: boolean;
  usagePercentage: number;
}

export interface DashboardEndpointStatus {
  id: number;
  name: string;
  url: string;
  projectName: string;
  status: EndpointStatus;
  uptimePercentage: number;
  avgLatencyMs: number;
  lastCheckAt?: string;
  lastFailureAt?: string;
  consecutiveFailures: number;
}

export interface IncidentSummary {
  id: number;
  endpointName: string;
  endpointUrl: string;
  failureType: string;
  startedAt: string;
  durationMinutes: number;
  failedCheckCount: number;
  lastErrorMessage?: string;
}

// ============================================================================
// API Response Wrapper
// ============================================================================

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  errors?: ErrorDetail[];
  timestamp: string;
}

export interface ErrorDetail {
  field?: string;
  code?: string;
  message: string;
}
