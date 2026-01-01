/**
 * Dashboard Page - Main monitoring overview
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Activity,
  ArrowUp,
  ArrowDown,
  AlertTriangle,
  Zap,
  TrendingUp,
  Bell,
  Plus,
  ExternalLink,
} from 'lucide-react';
import { Header } from '../components/layout';
import { Card, CardHeader, CardTitle, Button, StatusBadge, StatusDot } from '../components/common';
import { dashboardApi } from '../services/api';
import type { DashboardOverview, DashboardEndpointStatus, AlertListItem } from '../types';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

export const DashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<DashboardOverview | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const navigate = useNavigate();

  const fetchDashboard = async (showLoading = true) => {
    if (showLoading) setIsLoading(true);
    else setIsRefreshing(true);

    try {
      const response = await dashboardApi.getOverview();
      setDashboard(response.data.data);
    } catch (error) {
      toast.error('Failed to load dashboard');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
    // Auto-refresh every 30 seconds
    const interval = setInterval(() => fetchDashboard(false), 30000);
    return () => clearInterval(interval);
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p className="text-gray-500">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500">Failed to load dashboard</p>
          <Button onClick={() => fetchDashboard()} className="mt-4">
            Retry
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header
        title="Dashboard"
        subtitle="Monitor your API endpoints"
        onRefresh={() => fetchDashboard(false)}
        isRefreshing={isRefreshing}
      />

      <div className="p-6 space-y-6">
        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title="Total Endpoints"
            value={dashboard.totalEndpoints}
            icon={<Activity className="w-6 h-6" />}
            color="blue"
          />
          <StatCard
            title="Uptime"
            value={`${dashboard.overallUptimePercentage.toFixed(2)}%`}
            icon={<TrendingUp className="w-6 h-6" />}
            color="green"
            subtitle="Last 24 hours"
          />
          <StatCard
            title="Avg Latency"
            value={`${dashboard.avgLatencyMs.toFixed(0)}ms`}
            icon={<Zap className="w-6 h-6" />}
            color="yellow"
          />
          <StatCard
            title="Open Incidents"
            value={dashboard.openIncidents}
            icon={<AlertTriangle className="w-6 h-6" />}
            color="red"
            alert={dashboard.openIncidents > 0}
          />
        </div>

        {/* Status Overview */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <StatusCard
            title="Up"
            count={dashboard.upEndpoints}
            total={dashboard.totalEndpoints}
            color="green"
            icon={<ArrowUp className="w-5 h-5" />}
          />
          <StatusCard
            title="Down"
            count={dashboard.downEndpoints}
            total={dashboard.totalEndpoints}
            color="red"
            icon={<ArrowDown className="w-5 h-5" />}
          />
          <StatusCard
            title="Degraded"
            count={dashboard.degradedEndpoints}
            total={dashboard.totalEndpoints}
            color="yellow"
            icon={<AlertTriangle className="w-5 h-5" />}
          />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Endpoints List */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader action={
                <Button
                  size="sm"
                  onClick={() => navigate('/endpoints')}
                  leftIcon={<Plus className="w-4 h-4" />}
                >
                  Add Endpoint
                </Button>
              }>
                <CardTitle>Endpoints</CardTitle>
              </CardHeader>

              <div className="space-y-3">
                {dashboard.endpointStatuses.length === 0 ? (
                  <div className="text-center py-8">
                    <Activity className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                    <p className="text-gray-500">No endpoints configured</p>
                    <Button
                      variant="secondary"
                      className="mt-3"
                      onClick={() => navigate('/projects')}
                    >
                      Create Your First Project
                    </Button>
                  </div>
                ) : (
                  dashboard.endpointStatuses.slice(0, 5).map((endpoint) => (
                    <EndpointRow key={endpoint.id} endpoint={endpoint} />
                  ))
                )}
              </div>

              {dashboard.endpointStatuses.length > 5 && (
                <div className="mt-4 pt-4 border-t border-gray-100 text-center">
                  <Button
                    variant="ghost"
                    onClick={() => navigate('/endpoints')}
                    rightIcon={<ExternalLink className="w-4 h-4" />}
                  >
                    View All Endpoints
                  </Button>
                </div>
              )}
            </Card>
          </div>

          {/* Right Sidebar */}
          <div className="space-y-6">
            {/* Plan Usage */}
            <Card>
              <CardHeader>
                <CardTitle>Plan Usage</CardTitle>
              </CardHeader>
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-600">Endpoints</span>
                    <span className="font-medium">
                      {dashboard.planUsage.usedEndpoints} / {dashboard.planUsage.maxEndpoints}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-primary-600 h-2 rounded-full transition-all duration-500"
                      style={{ width: `${Math.min(dashboard.planUsage.usagePercentage, 100)}%` }}
                    ></div>
                  </div>
                </div>
                <div className="text-sm text-gray-500">
                  <p>Plan: <span className="font-medium text-gray-900">{dashboard.planUsage.planName}</span></p>
                  <p>Check Interval: {dashboard.planUsage.minCheckIntervalSeconds}s minimum</p>
                  <p>History: {dashboard.planUsage.historyDays} days</p>
                </div>
                {dashboard.planUsage.usagePercentage >= 80 && (
                  <Button variant="secondary" size="sm" className="w-full">
                    Upgrade Plan
                  </Button>
                )}
              </div>
            </Card>

            {/* Recent Alerts */}
            <Card>
              <CardHeader action={
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/alerts')}
                  rightIcon={<ExternalLink className="w-4 h-4" />}
                >
                  View All
                </Button>
              }>
                <CardTitle>Recent Alerts</CardTitle>
              </CardHeader>
              <div className="space-y-3">
                {dashboard.recentAlerts.length === 0 ? (
                  <div className="text-center py-6">
                    <Bell className="w-10 h-10 text-gray-300 mx-auto mb-2" />
                    <p className="text-sm text-gray-500">No recent alerts</p>
                  </div>
                ) : (
                  dashboard.recentAlerts.map((alert) => (
                    <AlertRow key={alert.id} alert={alert} />
                  ))
                )}
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

// Sub-components

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: 'blue' | 'green' | 'yellow' | 'red';
  subtitle?: string;
  alert?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  color,
  subtitle,
  alert,
}) => {
  const colors = {
    blue: 'bg-blue-100 text-blue-600',
    green: 'bg-green-100 text-green-600',
    yellow: 'bg-yellow-100 text-yellow-600',
    red: 'bg-red-100 text-red-600',
  };

  return (
    <Card className={alert ? 'ring-2 ring-red-200' : ''}>
      <div className="flex items-center gap-4">
        <div className={`p-3 rounded-xl ${colors[color]}`}>{icon}</div>
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          {subtitle && <p className="text-xs text-gray-400">{subtitle}</p>}
        </div>
      </div>
    </Card>
  );
};

interface StatusCardProps {
  title: string;
  count: number;
  total: number;
  color: 'green' | 'red' | 'yellow';
  icon: React.ReactNode;
}

const StatusCard: React.FC<StatusCardProps> = ({ title, count, total, color, icon }) => {
  const colors = {
    green: 'text-green-600 bg-green-50',
    red: 'text-red-600 bg-red-50',
    yellow: 'text-yellow-600 bg-yellow-50',
  };

  const percentage = total > 0 ? (count / total) * 100 : 0;

  return (
    <Card>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-lg ${colors[color]}`}>{icon}</div>
          <div>
            <p className="text-sm text-gray-500">{title}</p>
            <p className="text-xl font-bold text-gray-900">{count}</p>
          </div>
        </div>
        <span className="text-2xl font-bold text-gray-300">
          {percentage.toFixed(0)}%
        </span>
      </div>
    </Card>
  );
};

const EndpointRow: React.FC<{ endpoint: DashboardEndpointStatus }> = ({ endpoint }) => {
  return (
    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
      <div className="flex items-center gap-3">
        <StatusDot status={endpoint.status} pulse />
        <div>
          <p className="font-medium text-gray-900">{endpoint.name}</p>
          <p className="text-sm text-gray-500 truncate max-w-xs">{endpoint.url}</p>
        </div>
      </div>
      <div className="flex items-center gap-4 text-sm">
        <div className="text-right">
          <p className="font-medium">{endpoint.uptimePercentage.toFixed(1)}%</p>
          <p className="text-gray-400">uptime</p>
        </div>
        <div className="text-right">
          <p className="font-medium">{endpoint.avgLatencyMs.toFixed(0)}ms</p>
          <p className="text-gray-400">latency</p>
        </div>
        <StatusBadge status={endpoint.status} size="sm" />
      </div>
    </div>
  );
};

const AlertRow: React.FC<{ alert: AlertListItem }> = ({ alert }) => {
  const severityColors = {
    CRITICAL: 'text-red-600 bg-red-100',
    ERROR: 'text-orange-600 bg-orange-100',
    WARNING: 'text-yellow-600 bg-yellow-100',
    INFO: 'text-blue-600 bg-blue-100',
  };

  return (
    <div className={`p-3 rounded-lg ${alert.acknowledged ? 'bg-gray-50' : 'bg-red-50'}`}>
      <div className="flex items-start gap-2">
        <span className={`px-2 py-0.5 text-xs font-medium rounded ${severityColors[alert.severity]}`}>
          {alert.severity}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">{alert.title}</p>
          <p className="text-xs text-gray-500">
            {formatDistanceToNow(new Date(alert.createdAt), { addSuffix: true })}
          </p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
