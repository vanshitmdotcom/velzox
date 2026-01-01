/**
 * Status Badge Component - Visual indicator for endpoint status
 */

import React from 'react';
import { clsx } from 'clsx';
import { CheckCircle, XCircle, AlertTriangle, HelpCircle } from 'lucide-react';
import type { EndpointStatus } from '../../types';

interface StatusBadgeProps {
  status: EndpointStatus;
  size?: 'sm' | 'md' | 'lg';
  showIcon?: boolean;
  showLabel?: boolean;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  size = 'md',
  showIcon = true,
  showLabel = true,
}) => {
  const statusConfig = {
    UP: {
      label: 'Up',
      bgColor: 'bg-green-100',
      textColor: 'text-green-800',
      dotColor: 'bg-green-500',
      Icon: CheckCircle,
    },
    DOWN: {
      label: 'Down',
      bgColor: 'bg-red-100',
      textColor: 'text-red-800',
      dotColor: 'bg-red-500',
      Icon: XCircle,
    },
    DEGRADED: {
      label: 'Degraded',
      bgColor: 'bg-yellow-100',
      textColor: 'text-yellow-800',
      dotColor: 'bg-yellow-500',
      Icon: AlertTriangle,
    },
    UNKNOWN: {
      label: 'Unknown',
      bgColor: 'bg-gray-100',
      textColor: 'text-gray-800',
      dotColor: 'bg-gray-400',
      Icon: HelpCircle,
    },
  };

  const config = statusConfig[status];
  const sizes = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-sm',
    lg: 'px-3 py-1.5 text-base',
  };

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-4 h-4',
    lg: 'w-5 h-5',
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center font-medium rounded-full',
        config.bgColor,
        config.textColor,
        sizes[size]
      )}
    >
      {showIcon && (
        <config.Icon className={clsx(iconSizes[size], showLabel && 'mr-1')} />
      )}
      {showLabel && config.label}
    </span>
  );
};

/**
 * Status Dot - Simple colored dot for status indication
 */
interface StatusDotProps {
  status: EndpointStatus;
  size?: 'sm' | 'md' | 'lg';
  pulse?: boolean;
}

export const StatusDot: React.FC<StatusDotProps> = ({
  status,
  size = 'md',
  pulse = false,
}) => {
  const colors = {
    UP: 'bg-green-500',
    DOWN: 'bg-red-500',
    DEGRADED: 'bg-yellow-500',
    UNKNOWN: 'bg-gray-400',
  };

  const sizes = {
    sm: 'w-2 h-2',
    md: 'w-3 h-3',
    lg: 'w-4 h-4',
  };

  return (
    <span className="relative inline-flex">
      <span
        className={clsx(
          'rounded-full',
          colors[status],
          sizes[size],
          pulse && status === 'DOWN' && 'animate-pulse'
        )}
      />
      {pulse && status === 'DOWN' && (
        <span
          className={clsx(
            'absolute inline-flex h-full w-full rounded-full opacity-75 animate-ping',
            colors[status]
          )}
        />
      )}
    </span>
  );
};

export default StatusBadge;
