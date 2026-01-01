/**
 * Sidebar Component - Main navigation sidebar
 */

import React from 'react';
import { NavLink } from 'react-router-dom';
import { clsx } from 'clsx';
import {
  LayoutDashboard,
  FolderOpen,
  Globe,
  Key,
  Bell,
  Settings,
  HelpCircle,
  LogOut,
  Activity,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface NavItemProps {
  to: string;
  icon: React.ReactNode;
  label: string;
  badge?: number;
}

const NavItem: React.FC<NavItemProps> = ({ to, icon, label, badge }) => {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
          isActive
            ? 'bg-primary-50 text-primary-700'
            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
        )
      }
    >
      <span className="w-5 h-5">{icon}</span>
      <span className="flex-1">{label}</span>
      {badge !== undefined && badge > 0 && (
        <span className="px-2 py-0.5 text-xs font-medium bg-red-100 text-red-700 rounded-full">
          {badge}
        </span>
      )}
    </NavLink>
  );
};

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();

  const mainNavItems = [
    { to: '/dashboard', icon: <LayoutDashboard />, label: 'Dashboard' },
    { to: '/projects', icon: <FolderOpen />, label: 'Projects' },
    { to: '/endpoints', icon: <Globe />, label: 'Endpoints' },
    { to: '/credentials', icon: <Key />, label: 'Credentials' },
    { to: '/alerts', icon: <Bell />, label: 'Alerts' },
  ];

  const bottomNavItems = [
    { to: '/settings', icon: <Settings />, label: 'Settings' },
    { to: '/help', icon: <HelpCircle />, label: 'Help & Support' },
  ];

  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-white border-r border-gray-200 flex flex-col z-30">
      {/* Logo */}
      <div className="h-16 flex items-center gap-2 px-6 border-b border-gray-100">
        <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
          <Activity className="w-5 h-5 text-white" />
        </div>
        <span className="text-lg font-bold text-gray-900">API Monitor</span>
      </div>

      {/* Main Navigation */}
      <nav className="flex-1 px-4 py-6 overflow-y-auto">
        <div className="space-y-1">
          {mainNavItems.map((item) => (
            <NavItem key={item.to} {...item} />
          ))}
        </div>
      </nav>

      {/* Bottom Navigation */}
      <div className="px-4 py-4 border-t border-gray-100">
        <div className="space-y-1">
          {bottomNavItems.map((item) => (
            <NavItem key={item.to} {...item} />
          ))}
        </div>

        {/* User Info & Logout */}
        <div className="mt-4 pt-4 border-t border-gray-100">
          <div className="px-3 py-2">
            <p className="text-sm font-medium text-gray-900 truncate">
              {user?.name}
            </p>
            <p className="text-xs text-gray-500 truncate">{user?.email}</p>
            <span className="inline-flex items-center mt-1 px-2 py-0.5 text-xs font-medium bg-primary-100 text-primary-700 rounded">
              {user?.plan} Plan
            </span>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2.5 mt-2 rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-700 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
