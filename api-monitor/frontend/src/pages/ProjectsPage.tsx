/**
 * Projects Page - Manage monitoring projects
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FolderOpen,
  Plus,
  MoreVertical,
  Globe,
  Trash2,
  Edit,
  ArrowRight,
} from 'lucide-react';
import { Header } from '../components/layout';
import {
  Card,
  Button,
  Modal,
  ModalFooter,
  Input,
  StatusDot,
} from '../components/common';
import { projectApi } from '../services/api';
import type { ProjectListItem } from '../types';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

export const ProjectsPage: React.FC = () => {
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<ProjectListItem | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<ProjectListItem | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const fetchProjects = async () => {
    try {
      const response = await projectApi.getAll();
      setProjects(response.data.data);
    } catch (error) {
      toast.error('Failed to load projects');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const openCreateModal = () => {
    setEditingProject(null);
    setFormData({ name: '', description: '' });
    setIsModalOpen(true);
  };

  const openEditModal = (project: ProjectListItem) => {
    setEditingProject(project);
    setFormData({ name: project.name, description: project.description || '' });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      toast.error('Project name is required');
      return;
    }

    setIsSubmitting(true);
    try {
      if (editingProject) {
        await projectApi.update(editingProject.id, formData);
        toast.success('Project updated');
      } else {
        await projectApi.create(formData);
        toast.success('Project created');
      }
      setIsModalOpen(false);
      fetchProjects();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Operation failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;

    setIsSubmitting(true);
    try {
      await projectApi.delete(deleteConfirm.id);
      toast.success('Project deleted');
      setDeleteConfirm(null);
      fetchProjects();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Delete failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p className="text-gray-500">Loading projects...</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header title="Projects" subtitle="Organize your monitored endpoints" />

      <div className="p-6">
        {/* Action Bar */}
        <div className="flex justify-between items-center mb-6">
          <p className="text-gray-600">
            {projects.length} project{projects.length !== 1 ? 's' : ''}
          </p>
          <Button onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
            New Project
          </Button>
        </div>

        {/* Projects Grid */}
        {projects.length === 0 ? (
          <Card className="text-center py-12">
            <FolderOpen className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              No projects yet
            </h3>
            <p className="text-gray-500 mb-4 max-w-sm mx-auto">
              Projects help you organize your endpoints by application or service.
            </p>
            <Button onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
              Create Your First Project
            </Button>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map((project) => (
              <ProjectCard
                key={project.id}
                project={project}
                onEdit={() => openEditModal(project)}
                onDelete={() => setDeleteConfirm(project)}
                onView={() => navigate(`/projects/${project.id}`)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingProject ? 'Edit Project' : 'Create New Project'}
        size="md"
      >
        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            <Input
              label="Project Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., E-commerce Backend"
              required
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description (optional)
              </label>
              <textarea
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                className="w-full rounded-lg border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                rows={3}
                placeholder="Brief description of this project..."
              />
            </div>
          </div>
          <ModalFooter>
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              {editingProject ? 'Save Changes' : 'Create Project'}
            </Button>
          </ModalFooter>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Delete Project"
        size="sm"
      >
        <div className="text-center">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Trash2 className="w-6 h-6 text-red-600" />
          </div>
          <p className="text-gray-600 mb-2">
            Are you sure you want to delete{' '}
            <span className="font-medium text-gray-900">{deleteConfirm?.name}</span>?
          </p>
          <p className="text-sm text-gray-500">
            This will permanently delete all endpoints and data in this project.
          </p>
        </div>
        <ModalFooter>
          <Button variant="secondary" onClick={() => setDeleteConfirm(null)}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDelete} isLoading={isSubmitting}>
            Delete Project
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

// Sub-component

interface ProjectCardProps {
  project: ProjectListItem;
  onEdit: () => void;
  onDelete: () => void;
  onView: () => void;
}

const ProjectCard: React.FC<ProjectCardProps> = ({
  project,
  onEdit,
  onDelete,
  onView,
}) => {
  const [showMenu, setShowMenu] = useState(false);

  return (
    <Card hover className="relative">
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
            <div
              className="fixed inset-0 z-10"
              onClick={() => setShowMenu(false)}
            />
            <div className="absolute right-0 mt-1 w-36 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-20">
              <button
                onClick={() => {
                  setShowMenu(false);
                  onEdit();
                }}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Edit className="w-4 h-4" /> Edit
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

      {/* Content */}
      <div className="pr-8">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
            <FolderOpen className="w-5 h-5 text-primary-600" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">{project.name}</h3>
            <p className="text-sm text-gray-500">
              {formatDistanceToNow(new Date(project.createdAt), { addSuffix: true })}
            </p>
          </div>
        </div>

        {project.description && (
          <p className="text-sm text-gray-600 mb-4 line-clamp-2">
            {project.description}
          </p>
        )}

        {/* Stats */}
        <div className="flex items-center gap-4 text-sm mb-4">
          <div className="flex items-center gap-2">
            <Globe className="w-4 h-4 text-gray-400" />
            <span className="text-gray-600">
              {project.endpointCount} endpoint{project.endpointCount !== 1 ? 's' : ''}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <StatusDot status="UP" />
            <span className="text-green-600">{project.upCount}</span>
          </div>
          <div className="flex items-center gap-2">
            <StatusDot status="DOWN" />
            <span className="text-red-600">{project.downCount}</span>
          </div>
        </div>

        {/* View Button */}
        <Button
          variant="secondary"
          className="w-full"
          onClick={onView}
          rightIcon={<ArrowRight className="w-4 h-4" />}
        >
          View Project
        </Button>
      </div>
    </Card>
  );
};

export default ProjectsPage;
