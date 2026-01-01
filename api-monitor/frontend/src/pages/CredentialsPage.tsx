/**
 * Credentials Page - Manage API authentication credentials
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Key,
  Plus,
  MoreVertical,
  Trash2,
  Eye,
  EyeOff,
  Shield,
} from 'lucide-react';
import { Header } from '../components/layout';
import {
  Card,
  Button,
  Modal,
  ModalFooter,
  Input,
  Select,
} from '../components/common';
import { credentialApi, projectApi } from '../services/api';
import type { Credential, ProjectListItem, CredentialType } from '../types';
import toast from 'react-hot-toast';

const CREDENTIAL_TYPES: { value: CredentialType; label: string; description: string }[] = [
  { value: 'BEARER_TOKEN', label: 'Bearer Token', description: 'Authorization: Bearer <token>' },
  { value: 'API_KEY', label: 'API Key', description: 'Custom header with API key' },
  { value: 'BASIC_AUTH', label: 'Basic Auth', description: 'Username and password' },
];

export const CredentialsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const projectIdParam = searchParams.get('projectId');

  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(
    projectIdParam ? parseInt(projectIdParam) : null
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<Credential | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showValue, setShowValue] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    type: 'BEARER_TOKEN' as CredentialType,
    value: '',
    headerName: '',
    username: '',
    description: '',
  });

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const response = await projectApi.getAll();
        setProjects(response.data.data);
        if (!selectedProjectId && response.data.data.length > 0) {
          setSelectedProjectId(response.data.data[0].id);
        }
      } catch (error) {
        toast.error('Failed to load projects');
      }
    };
    fetchProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      fetchCredentials();
    }
  }, [selectedProjectId]);

  const fetchCredentials = async () => {
    if (!selectedProjectId) return;
    setIsLoading(true);
    try {
      const response = await credentialApi.getByProject(selectedProjectId);
      setCredentials(response.data.data);
    } catch (error) {
      toast.error('Failed to load credentials');
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateModal = () => {
    setFormData({
      name: '',
      type: 'BEARER_TOKEN',
      value: '',
      headerName: 'X-API-Key',
      username: '',
      description: '',
    });
    setShowValue(false);
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
      await credentialApi.create({
        ...formData,
        projectId: selectedProjectId,
      });
      toast.success('Credential created');
      setIsModalOpen(false);
      fetchCredentials();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to create credential');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    setIsSubmitting(true);
    try {
      await credentialApi.delete(deleteConfirm.id);
      toast.success('Credential deleted');
      setDeleteConfirm(null);
      fetchCredentials();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Delete failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (projects.length === 0 && !isLoading) {
    return (
      <div>
        <Header title="Credentials" subtitle="Manage API authentication" />
        <div className="p-6">
          <Card className="text-center py-12">
            <Key className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No projects yet
            </h3>
            <p className="text-gray-500 mb-4">
              Create a project first to add credentials.
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
      <Header title="Credentials" subtitle="Manage API authentication securely" />

      <div className="p-6">
        {/* Info Banner */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <div className="flex items-start gap-3">
            <Shield className="w-5 h-5 text-blue-600 mt-0.5" />
            <div>
              <h4 className="font-medium text-blue-900">Secure Storage</h4>
              <p className="text-sm text-blue-700 mt-1">
                All credentials are encrypted at rest using AES-256 and only decrypted in memory during API checks.
                Use service/health tokens instead of user login credentials.
              </p>
            </div>
          </div>
        </div>

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
              {credentials.length} credential{credentials.length !== 1 ? 's' : ''}
            </span>
          </div>
          <Button
            onClick={openCreateModal}
            leftIcon={<Plus className="w-4 h-4" />}
            disabled={!selectedProjectId}
          >
            Add Credential
          </Button>
        </div>

        {/* Credentials List */}
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="w-8 h-8 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          </div>
        ) : credentials.length === 0 ? (
          <Card className="text-center py-12">
            <Key className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No credentials yet
            </h3>
            <p className="text-gray-500 mb-4">
              Add credentials to authenticate your API endpoints.
            </p>
            <Button onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
              Add Credential
            </Button>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {credentials.map((credential) => (
              <CredentialCard
                key={credential.id}
                credential={credential}
                onDelete={() => setDeleteConfirm(credential)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add New Credential"
        size="md"
      >
        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            <Input
              label="Credential Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., Production API Key"
              required
            />

            <Select
              label="Type"
              options={CREDENTIAL_TYPES.map((t) => ({ value: t.value, label: t.label }))}
              value={formData.type}
              onChange={(e) =>
                setFormData({ ...formData, type: e.target.value as CredentialType })
              }
            />
            <p className="text-xs text-gray-500 -mt-3">
              {CREDENTIAL_TYPES.find((t) => t.value === formData.type)?.description}
            </p>

            {formData.type === 'API_KEY' && (
              <Input
                label="Header Name"
                value={formData.headerName}
                onChange={(e) => setFormData({ ...formData, headerName: e.target.value })}
                placeholder="X-API-Key"
                required
              />
            )}

            {formData.type === 'BASIC_AUTH' && (
              <Input
                label="Username"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                placeholder="username"
                required
              />
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {formData.type === 'BASIC_AUTH' ? 'Password' : 'Token/Key Value'}
              </label>
              <div className="relative">
                <input
                  type={showValue ? 'text' : 'password'}
                  value={formData.value}
                  onChange={(e) => setFormData({ ...formData, value: e.target.value })}
                  className="w-full rounded-lg border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 pr-10"
                  placeholder="Enter your secret value"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowValue(!showValue)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showValue ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description (optional)
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full rounded-lg border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                rows={2}
                placeholder="What is this credential used for?"
              />
            </div>
          </div>

          <ModalFooter>
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Create Credential
            </Button>
          </ModalFooter>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <Modal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Delete Credential"
        size="sm"
      >
        <div className="text-center">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Trash2 className="w-6 h-6 text-red-600" />
          </div>
          <p className="text-gray-600">
            Delete <span className="font-medium">{deleteConfirm?.name}</span>?
          </p>
          {deleteConfirm?.inUse && (
            <p className="text-sm text-red-600 mt-2">
              ⚠️ This credential is in use by endpoints and cannot be deleted.
            </p>
          )}
        </div>
        <ModalFooter>
          <Button variant="secondary" onClick={() => setDeleteConfirm(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleDelete}
            isLoading={isSubmitting}
            disabled={deleteConfirm?.inUse}
          >
            Delete
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

// Sub-component

const CredentialCard: React.FC<{ credential: Credential; onDelete: () => void }> = ({
  credential,
  onDelete,
}) => {
  const [showMenu, setShowMenu] = useState(false);

  const typeLabels = {
    BEARER_TOKEN: 'Bearer Token',
    API_KEY: 'API Key',
    BASIC_AUTH: 'Basic Auth',
  };

  return (
    <Card className="relative">
      {/* Menu */}
      <div className="absolute top-4 right-4">
        <button
          onClick={() => setShowMenu(!showMenu)}
          className="p-1 text-gray-400 hover:text-gray-600 rounded"
        >
          <MoreVertical className="w-5 h-5" />
        </button>
        {showMenu && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setShowMenu(false)} />
            <div className="absolute right-0 mt-1 w-32 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-20">
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

      <div className="pr-8">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
            <Key className="w-5 h-5 text-gray-600" />
          </div>
          <div>
            <h4 className="font-medium text-gray-900">{credential.name}</h4>
            <span className="text-xs text-gray-500">
              {typeLabels[credential.type]}
            </span>
          </div>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex items-center justify-between">
            <span className="text-gray-500">Value:</span>
            <code className="bg-gray-100 px-2 py-1 rounded text-gray-600">
              {credential.maskedValue}
            </code>
          </div>
          {credential.headerName && (
            <div className="flex items-center justify-between">
              <span className="text-gray-500">Header:</span>
              <code className="text-gray-600">{credential.headerName}</code>
            </div>
          )}
          {credential.inUse && (
            <span className="inline-flex items-center px-2 py-0.5 rounded bg-green-100 text-green-700 text-xs">
              In use
            </span>
          )}
        </div>

        {credential.description && (
          <p className="text-sm text-gray-500 mt-3 line-clamp-2">
            {credential.description}
          </p>
        )}
      </div>
    </Card>
  );
};

export default CredentialsPage;
