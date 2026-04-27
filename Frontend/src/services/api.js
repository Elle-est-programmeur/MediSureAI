import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';
const AUTH_BASE_URL = 'http://localhost:8080/auth';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ─── Request Interceptor: attach Bearer token ─────────────────────────────────
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && token.length > 25 && !token.includes('[object')) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// ─── Response Interceptor: unwrap data + handle 401 ───────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn("Session expired or unauthorized - clearing storage");
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════════════════════
export const register = async (userData) => {
  const response = await axios.post(`${AUTH_BASE_URL}/register`, userData);
  return response.data;
};

export const login = async (credentials) => {
  const response = await axios.post(`${AUTH_BASE_URL}/login`, credentials);
  return response.data;
};

export const refreshToken = async (token) => {
  const response = await axios.post(`${AUTH_BASE_URL}/refresh`, { refreshToken: token });
  return response.data;
};

export const logoutApi = async () => {
  const token = localStorage.getItem('token');
  const response = await axios.post(`${AUTH_BASE_URL}/logout`, {}, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

// ═══════════════════════════════════════════════════════════════════════════════
// DOCUMENTS (existing + consolidated)
// ═══════════════════════════════════════════════════════════════════════════════
export const documentAPI = {
  upload: async (files, documentType = 'MEDICAL_REPORT') => {
    const formData = new FormData();
    Array.from(files).forEach((f) => formData.append('files', f));
    formData.append('documentType', documentType);
    const response = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  list: async () => {
    const response = await api.get('/documents');
    return response.data;
  },

  getCompleted: async () => {
    const response = await api.get('/documents/completed');
    return response.data;
  },

  delete: async (id) => {
    const response = await api.delete(`/documents/${id}`);
    return response.data;
  },

  clear: async () => {
    const response = await api.delete('/documents/clear');
    return response.data;
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// SRLM (existing)
// ═══════════════════════════════════════════════════════════════════════════════
export const srlmAPI = {
  query: async (question, sessionId) => {
    const response = await api.post('/srlm', { query: question, sessionId });
    return response.data;
  },

  debug: async (question, sessionId) => {
    const response = await api.post('/srlm/debug', { query: question, sessionId });
    return response.data;
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// DOCTOR API (Bearer token, role=DOCTOR)
// ═══════════════════════════════════════════════════════════════════════════════
export const doctorAPI = {
  getProfile: async () => {
    const response = await api.get('/doctor/profile');
    return response.data;
  },

  updateProfile: async (data) => {
    const response = await api.post('/doctor/profile', data);
    return response.data;
  },

  getPatients: async () => {
    const response = await api.get('/doctor/patients');
    return response.data;
  },

  createRecord: async (data) => {
    const response = await api.post('/doctor/records', data);
    return response.data;
  },

  getRecord: async (id) => {
    const response = await api.get(`/doctor/records/${id}`);
    return response.data;
  },

  updateRecord: async (id, data) => {
    const response = await api.put(`/doctor/records/${id}`, data);
    return response.data;
  },

  deleteRecord: async (id) => {
    const response = await api.delete(`/doctor/records/${id}`);
    return response.data;
  },

  createBilling: async (data) => {
    const response = await api.post('/doctor/billing', data);
    return response.data;
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// PATIENT API (Bearer token, role=PATIENT)
// ═══════════════════════════════════════════════════════════════════════════════
export const patientAPI = {
  getProfile: async () => {
    const response = await api.get('/patient/profile');
    return response.data;
  },

  updateProfile: async (data) => {
    const response = await api.post('/patient/profile', data);
    return response.data;
  },

  getRecords: async () => {
    const response = await api.get('/patient/records');
    return response.data;
  },

  getBilling: async () => {
    const response = await api.get('/patient/billing');
    return response.data;
  },

  chat: async (query, sessionId) => {
    const response = await api.post('/patient/chat', { query, sessionId });
    return response.data;
  },

  getTimeline: async () => {
    const response = await api.get('/patient/timeline');
    return response.data;
  },

  formularySearch: async (drug) => {
    const response = await api.get('/patient/formulary-search', { params: { drug } });
    return response.data;
  },
};

// ═══════════════════════════════════════════════════════════════════════════════
// LEGACY EXPORTS (backward compat with existing components)
// ═══════════════════════════════════════════════════════════════════════════════
export const uploadDocuments = documentAPI.upload;
export const clearDocuments = documentAPI.clear;
export const getDocuments = documentAPI.list;
export const getCompletedDocuments = documentAPI.getCompleted;
export const askQuestion = srlmAPI.query;
export const getTimeline = patientAPI.getTimeline;
export const searchFormulary = patientAPI.formularySearch;

export default api;
