/**
 * Alerts Page - View and manage alert history
 */

import React, { useState, useEffect } from 'react';
import {
  Bell,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Info,
  Clock,
} from 'lucide-react';
import { Header } from '../components/layout';
import { Card, Button } from '../components/common';
import { alertApi } from '../services/api';
import type { Alert } from '../types';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

export const AlertsPage: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'unacknowledged'>('all');

  const fetchAlerts = async () => {
    setIsLoading(true);
    try {
      const response = await alertApi.getAll();
      setAlerts(response.data.data);
    } catch (error) {
      toast.error('Failed to load alerts');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleAcknowledge = async (alertId: number) => {
    try {
      await alertApi.acknowledge(alertId);
      toast.success('Alert acknowledged');
      fetchAlerts();
    } catch (error) {
      toast.error('Failed to acknowledge alert');
    }
  };

  const filteredAlerts = filter === 'unacknowledged'
    ? alerts.filter((a) => !a.acknowledged)
    : alerts;

  const unacknowledgedCount = alerts.filter((a) => !a.acknowledged).length;

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p className="text-gray-500">Loading alerts...</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header title="Alerts" subtitle="View and manage alert notifications" />

      <div className="p-6">
        {/* Filters */}
        <div className="flex items-center gap-4 mb-6">
          <div className="flex bg-gray-100 rounded-lg p-1">
            <button
              onClick={() => setFilter('all')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                filter === 'all'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              All Alerts
            </button>
            <button
              onClick={() => setFilter('unacknowledged')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors flex items-center gap-2 ${
                filter === 'unacknowledged'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Unacknowledged
              {unacknowledgedCount > 0 && (
                <span className="px-2 py-0.5 text-xs bg-red-100 text-red-700 rounded-full">
                  {unacknowledgedCount}
                </span>
              )}
            </button>
          </div>
          <span className="text-gray-500">
            {filteredAlerts.length} alert{filteredAlerts.length !== 1 ? 's' : ''}
          </span>
        </div>

        {/* Alerts List */}
        {filteredAlerts.length === 0 ? (
          <Card className="text-center py-12">
            <Bell className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {filter === 'unacknowledged' ? 'All caught up!' : 'No alerts yet'}
            </h3>
            <p className="text-gray-500">
              {filter === 'unacknowledged'
                ? 'You have no unacknowledged alerts.'
                : 'Alerts will appear here when your endpoints have issues.'}
            </p>
          </Card>
        ) : (
          <div className="space-y-4">
            {filteredAlerts.map((alert) => (
              <AlertCard
                key={alert.id}
                alert={alert}
                onAcknowledge={() => handleAcknowledge(alert.id)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// Sub-component

interface AlertCardProps {
  alert: Alert;
  onAcknowledge: () => void;
}

const AlertCard: React.FC<AlertCardProps> = ({ alert, onAcknowledge }) => {
  const severityConfig = {
    CRITICAL: {
      bg: 'bg-red-50 border-red-200',
      icon: XCircle,
      iconColor: 'text-red-600',
      badge: 'bg-red-100 text-red-700',
    },
    ERROR: {
      bg: 'bg-orange-50 border-orange-200',
      icon: AlertTriangle,
      iconColor: 'text-orange-600',
      badge: 'bg-orange-100 text-orange-700',
    },
    WARNING: {
      bg: 'bg-yellow-50 border-yellow-200',
      icon: AlertTriangle,
      iconColor: 'text-yellow-600',
      badge: 'bg-yellow-100 text-yellow-700',
    },
    INFO: {
      bg: 'bg-blue-50 border-blue-200',
      icon: Info,
      iconColor: 'text-blue-600',
      badge: 'bg-blue-100 text-blue-700',
    },
  };

  const config = severityConfig[alert.severity];
  const Icon = config.icon;

  const typeLabels: Record<string, string> = {
    ENDPOINT_DOWN: 'Endpoint Down',
    ENDPOINT_RECOVERED: 'Recovered',
    AUTH_FAILURE: 'Auth Failure',
    TIMEOUT: 'Timeout',
    SSL_ERROR: 'SSL Error',
    LATENCY_BREACH: 'Slow Response',
    CONNECTION_ERROR: 'Connection Error',
  };

  return (
    <Card
      padding="none"
      className={`border ${
        alert.acknowledged ? 'bg-white' : config.bg
      } transition-colors`}
    >
      <div className="p-4">
        <div className="flex items-start gap-4">
          <div className={`p-2 rounded-lg ${alert.acknowledged ? 'bg-gray-100' : ''}`}>
            <Icon className={`w-6 h-6 ${alert.acknowledged ? 'text-gray-400' : config.iconColor}`} />
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className={`px-2 py-0.5 text-xs font-medium rounded ${config.badge}`}>
                {alert.severity}
              </span>
              <span className="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-600 rounded">
                {typeLabels[alert.type] || alert.type}
              </span>
              {alert.acknowledged && (
                <span className="flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded">
                  <CheckCircle className="w-3 h-3" /> Acknowledged
                </span>
              )}
            </div>

            <h4 className="font-medium text-gray-900 mb-1">{alert.title}</h4>

            <div className="flex items-center gap-4 text-sm text-gray-500 mb-2">
              <span className="flex items-center gap-1">
                <Clock className="w-4 h-4" />
                {formatDistanceToNow(new Date(alert.createdAt), { addSuffix: true })}
              </span>
              <span className="truncate">{alert.endpointUrl}</span>
            </div>

            <div className="bg-gray-50 rounded-lg p-3 text-sm text-gray-600 font-mono whitespace-pre-wrap">
              {alert.message}
            </div>

            {!alert.delivered && alert.deliveryError && (
              <p className="mt-2 text-sm text-red-600">
                ⚠️ Delivery failed: {alert.deliveryError}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            {!alert.acknowledged && (
              <Button size="sm" variant="secondary" onClick={onAcknowledge}>
                Acknowledge
              </Button>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};

export default AlertsPage;
