/**
 * Endpoints Page - Manage monitored API endpoints
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Globe,
  Plus,
  MoreVertical,
  Trash2,
  Power,
  ExternalLink,
  Activity,
} from 'lucide-react';
import { Header } from '../components/layout';
import {
  Card,
  Button,
  Modal,
  ModalFooter,
  Input,
  Select,
  StatusBadge,
  StatusDot,
} from '../components/common';
import { endpointApi, projectApi, credentialApi } from '../services/api';
import type { EndpointListItem, ProjectListItem, Credential, HttpMethod } from '../types';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

const HTTP_METHODS: { value: HttpMethod; label: string }[] = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
];

const CHECK_INTERVALS = [
  { value: 30, label: '30 seconds (PRO)' },
  { value: 60, label: '1 minute (Starter+)' },
  { value: 300, label: '5 minutes' },
  { value: 600, label: '10 minutes' },
  { value: 1800, label: '30 minutes' },
  { value: 3600, label: '1 hour' },
];

export const EndpointsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const projectIdParam = searchParams.get('projectId');

  const [endpoints, setEndpoints] = useState<EndpointListItem[]>([]);
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(
    projectIdParam ? parseInt(projectIdParam) : null
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<EndpointListItem | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    url: '',
    method: 'GET' as HttpMethod,
    expectedStatusCode: 200,
    checkIntervalSeconds: 300,
    timeoutMs: 30000,
    headers: '',
    requestBody: '',
    maxLatencyMs: '',
    credentialId: '',
  });

  // Fetch projects on mount
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const response = await projectApi.getAll();
        setProjects(response.data.data);
        // Auto-select first project if none selected
        if (!selectedProjectId && response.data.data.length > 0) {
          setSelectedProjectId(response.data.data[0].id);
        }
      } catch (error) {
        toast.error('Failed to load projects');
      }
    };
    fetchProjects();
  }, []);

  // Fetch endpoints when project changes
  useEffect(() => {
    if (selectedProjectId) {
      fetchEndpoints();
      fetchCredentials();
    }
  }, [selectedProjectId]);

  const fetchEndpoints = async () => {
    if (!selectedProjectId) return;
    setIsLoading(true);
    try {
      const response = await endpointApi.getByProject(selectedProjectId);
      setEndpoints(response.data.data);
    } catch (error) {
      toast.error('Failed to load endpoints');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchCredentials = async () => {
    if (!selectedProjectId) return;
    try {
      const response = await credentialApi.getByProject(selectedProjectId);
      setCredentials(response.data.data);
    } catch (error) {
      console.error('Failed to load credentials');
    }
  };

  const openCreateModal = () => {
    setFormData({
      name: '',
      url: '',
      method: 'GET',
      expectedStatusCode: 200,
      checkIntervalSeconds: 300,
      timeoutMs: 30000,
      headers: '',
      requestBody: '',
      maxLatencyMs: '',
      credentialId: '',
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProjectId) {
      toast.error('Please select a project');
      return;
    }

    setIsSubmitting(true);
    try {
      await endpointApi.create({
        ...formData,
        projectId: selectedProjectId,
        maxLatencyMs: formData.maxLatencyMs ? parseInt(formData.maxLatencyMs) : undefined,
        credentialId: formData.credentialId ? parseInt(formData.credentialId) : undefined,
      });
      toast.success('Endpoint created');
      setIsModalOpen(false);
      fetchEndpoints();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to create endpoint');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggle = async (endpoint: EndpointListItem) => {
    try {
      await endpointApi.toggle(endpoint.id, !endpoint.enabled);
      toast.success(endpoint.enabled ? 'Endpoint disabled' : 'Endpoint enabled');
      fetchEndpoints();
    } catch (error) {
      toast.error('Failed to update endpoint');
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    setIsSubmitting(true);
    try {
      await endpointApi.delete(deleteConfirm.id);
      toast.success('Endpoint deleted');
      setDeleteConfirm(null);
      fetchEndpoints();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Delete failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (projects.length === 0 && !isLoading) {
    return (
      <div>
        <Header title="Endpoints" subtitle="Configure your monitored APIs" />
        <div className="p-6">
          <Card className="text-center py-12">
            <Globe className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No projects yet
            </h3>
            <p className="text-gray-500 mb-4">
              Create a project first to add endpoints.
            </p>
            <Button onClick={() => navigate('/projects')}>
              Create Project
            </Button>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header title="Endpoints" subtitle="Configure your monitored APIs" />

      <div className="p-6">
        {/* Project Selector & Actions */}
        <div className="flex flex-wrap gap-4 items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <Select
              options={projects.map((p) => ({ value: p.id, label: p.name }))}
              value={selectedProjectId || ''}
              onChange={(e) => setSelectedProjectId(parseInt(e.target.value))}
              className="w-48"
            />
            <span className="text-gray-500">
              {endpoints.length} endpoint{endpoints.length !== 1 ? 's' : ''}
            </span>
          </div>
          <Button
            onClick={openCreateModal}
            leftIcon={<Plus className="w-4 h-4" />}
            disabled={!selectedProjectId}
          >
            Add Endpoint
          </Button>
        </div>

        {/* Endpoints List */}
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="w-8 h-8 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          </div>
        ) : endpoints.length === 0 ? (
          <Card className="text-center py-12">
            <Activity className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No endpoints yet
            </h3>
            <p className="text-gray-500 mb-4">
              Add your first endpoint to start monitoring.
            </p>
            <Button onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
              Add Endpoint
            </Button>
          </Card>
        ) : (
          <div className="space-y-3">
            {endpoints.map((endpoint) => (
              <EndpointRow
                key={endpoint.id}
                endpoint={endpoint}
                onToggle={() => handleToggle(endpoint)}
                onDelete={() => setDeleteConfirm(endpoint)}
                onView={() => navigate(`/endpoints/${endpoint.id}`)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create Endpoint Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add New Endpoint"
        size="lg"
      >
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
            <Input
              label="Endpoint Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., User API Health Check"
              required
            />

            <div className="grid grid-cols-4 gap-4">
              <div className="col-span-1">
                <Select
                  label="Method"
                  options={HTTP_METHODS}
                  value={formData.method}
                  onChange={(e) =>
                    setFormData({ ...formData, method: e.target.value as HttpMethod })
                  }
                />
              </div>
              <div className="col-span-3">
                <Input
                  label="URL"
                  type="url"
                  value={formData.url}
                  onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                  placeholder="https://api.example.com/health"
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Expected Status Code"
                type="number"
                value={formData.expectedStatusCode}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    expectedStatusCode: parseInt(e.target.value),
                  })
                }
                min={100}
                max={599}
              />
              <Select
                label="Check Interval"
                options={CHECK_INTERVALS}
                value={formData.checkIntervalSeconds}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    checkIntervalSeconds: parseInt(e.target.value),
                  })
                }
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Timeout (ms)"
                type="number"
                value={formData.timeoutMs}
                onChange={(e) =>
                  setFormData({ ...formData, timeoutMs: parseInt(e.target.value) })
                }
                min={1000}
                max={60000}
              />
              <Input
                label="Max Latency Alert (ms)"
                type="number"
                value={formData.maxLatencyMs}
                onChange={(e) =>
                  setFormData({ ...formData, maxLatencyMs: e.target.value })
                }
                placeholder="Optional"
              />
            </div>

            {credentials.length > 0 && (
              <Select
                label="Authentication (optional)"
                options={[
                  { value: '', label: 'No authentication' },
                  ...credentials.map((c) => ({
                    value: c.id,
                    label: `${c.name} (${c.type})`,
                  })),
                ]}
                value={formData.credentialId}
                onChange={(e) =>
                  setFormData({ ...formData, credentialId: e.target.value })
                }
              />
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Headers (JSON, optional)
              </label>
              <textarea
                value={formData.headers}
                onChange={(e) => setFormData({ ...formData, headers: e.target.value })}
                className="w-full rounded-lg border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 font-mono text-sm"
                rows={2}
                placeholder='{"Content-Type": "application/json"}'
              />
            </div>

            {(formData.method === 'POST' || formData.method === 'PUT') && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Request Body (JSON, optional)
                </label>
                <textarea
                  value={formData.requestBody}
                  onChange={(e) =>
                    setFormData({ ...formData, requestBody: e.target.value })
                  }
                  className="w-full rounded-lg border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 font-mono text-sm"
                  rows={3}
                  placeholder='{"key": "value"}'
                />
              </div>
            )}
          </div>

          <ModalFooter>
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Create Endpoint
            </Button>
          </ModalFooter>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <Modal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Delete Endpoint"
        size="sm"
      >
        <div className="text-center">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Trash2 className="w-6 h-6 text-red-600" />
          </div>
          <p className="text-gray-600">
            Delete <span className="font-medium">{deleteConfirm?.name}</span>?
          </p>
          <p className="text-sm text-gray-500 mt-1">
            All monitoring data will be permanently deleted.
          </p>
        </div>
        <ModalFooter>
          <Button variant="secondary" onClick={() => setDeleteConfirm(null)}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDelete} isLoading={isSubmitting}>
            Delete
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

// Sub-component

interface EndpointRowProps {
  endpoint: EndpointListItem;
  onToggle: () => void;
  onDelete: () => void;
  onView: () => void;
}

const EndpointRow: React.FC<EndpointRowProps> = ({
  endpoint,
  onToggle,
  onDelete,
  onView,
}) => {
  const [showMenu, setShowMenu] = useState(false);

  return (
    <Card
      padding="sm"
      className={`transition-opacity ${!endpoint.enabled ? 'opacity-60' : ''}`}
    >
      <div className="flex items-center justify-between p-2">
        <div className="flex items-center gap-4 flex-1 min-w-0">
          <StatusDot status={endpoint.status} pulse={endpoint.enabled} />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h4 className="font-medium text-gray-900">{endpoint.name}</h4>
              <span className="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-600 rounded">
                {endpoint.method}
              </span>
              {!endpoint.enabled && (
                <span className="px-2 py-0.5 text-xs font-medium bg-yellow-100 text-yellow-700 rounded">
                  Disabled
                </span>
              )}
            </div>
            <p className="text-sm text-gray-500 truncate">{endpoint.url}</p>
          </div>
        </div>

        <div className="flex items-center gap-6">
          {/* Stats */}
          <div className="hidden md:flex items-center gap-6 text-sm">
            <div className="text-center">
              <p className="font-medium text-gray-900">
                {endpoint.uptimePercentage?.toFixed(1) || '--'}%
              </p>
              <p className="text-xs text-gray-400">Uptime</p>
            </div>
            <div className="text-center">
              <p className="font-medium text-gray-900">
                {endpoint.avgLatencyMs?.toFixed(0) || '--'}ms
              </p>
              <p className="text-xs text-gray-400">Latency</p>
            </div>
            {endpoint.lastCheckAt && (
              <div className="text-center">
                <p className="font-medium text-gray-900">
                  {formatDistanceToNow(new Date(endpoint.lastCheckAt), {
                    addSuffix: false,
                  })}
                </p>
                <p className="text-xs text-gray-400">Last check</p>
              </div>
            )}
          </div>

          <StatusBadge status={endpoint.status} size="sm" />

          {/* Menu */}
          <div className="relative">
            <button
              onClick={() => setShowMenu(!showMenu)}
              className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
            >
              <MoreVertical className="w-5 h-5" />
            </button>
            {showMenu && (
              <>
                <div
                  className="fixed inset-0 z-10"
                  onClick={() => setShowMenu(false)}
                />
                <div className="absolute right-0 mt-1 w-40 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-20">
                  <button
                    onClick={() => {
                      setShowMenu(false);
                      onView();
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                  >
                    <ExternalLink className="w-4 h-4" /> View Details
                  </button>
                  <button
                    onClick={() => {
                      setShowMenu(false);
                      onToggle();
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                  >
                    <Power className="w-4 h-4" />
                    {endpoint.enabled ? 'Disable' : 'Enable'}
                  </button>
                  <button
                    onClick={() => {
                      setShowMenu(false);
                      onDelete();
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                  >
                    <Trash2 className="w-4 h-4" /> Delete
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};

export default EndpointsPage;
