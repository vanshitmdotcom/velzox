import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Vite Configuration for API Monitor Frontend
 * 
 * Features:
 * - React plugin for JSX/TSX support
 * - Development proxy to backend
 * - Optimized production build
 */
export default defineConfig({
  plugins: [react()],
  
  // Development server configuration
  server: {
    port: 5173,
    host: true, // Allow external connections
    
    // Proxy API requests to backend during development
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  
  // Build configuration
  build: {
    outDir: 'dist',
    sourcemap: false,
    
    // Optimize chunk sizes
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          ui: ['lucide-react', 'clsx'],
          data: ['axios', '@tanstack/react-query', 'date-fns'],
        },
      },
    },
  },
  
  // Resolve aliases (optional)
  resolve: {
    alias: {
      '@': '/src',
    },
  },
})
