import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Auto-inject JWT into protected requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && token.length > 25 && !token.includes('[object')) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Handle global responses (like 401/403)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn("Session expired or unauthorized - clearing storage");
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (!window.location.pathname.startsWith('/login')) {
         window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export const uploadDocuments = async (files) => {
  const formData = new FormData();
  Array.from(files).forEach((f) => formData.append('files', f));
  formData.append('documentType', 'INSURANCE_POLICY');

  const response = await api.post(`/documents/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data;
};

export const clearDocuments = async () => {
  const response = await api.delete(`/documents/clear`);
  return response.data;
};

export const askQuestion = async (question) => {
  const response = await api.post('/srlm', {
    query: question,
  });

  return response.data;
};

export const getDocuments = async () => {
  const response = await api.get('/documents');
  return response.data;
};

export const getCompletedDocuments = async () => {
  const response = await api.get('/documents/completed');
  return response.data;
};

// Authentication APIs (auth endpoints are at root level, not under /api)
export const register = async (userData) => {
  const response = await axios.post('http://localhost:8080/auth/register', userData);
  return response.data;
};

export const login = async (credentials) => {
  const response = await axios.post('http://localhost:8080/auth/login', credentials);
  return response.data;
};

export default api;
