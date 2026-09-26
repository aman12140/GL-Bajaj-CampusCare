import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the browser talks to Vite (port 5173); Vite forwards /api and /uploads to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
});
