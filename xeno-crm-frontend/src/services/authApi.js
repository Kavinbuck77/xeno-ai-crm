import api from './api';

export const authApi = {
  login: async (username, password) => {
    const response = await api.post('/api/auth/login', { username, password });
    if (response.data && response.data.token) {
      localStorage.setItem('xeno_token', response.data.token);
      localStorage.setItem('xeno_username', response.data.username);
    }
    return response.data;
  },
  register: async (username, password) => {
    const response = await api.post('/api/auth/register', { username, password });
    return response.data;
  },
  logout: () => {
    localStorage.removeItem('xeno_token');
    localStorage.removeItem('xeno_username');
    window.location.href = '/login';
  },
  isAuthenticated: () => {
    return !!localStorage.getItem('xeno_token');
  },
  getUsername: () => {
    return localStorage.getItem('xeno_username') || '';
  }
};
