import api from './api';

export const analyticsApi = {
  getSummary: async () => {
    const response = await api.get('/analytics/summary');
    return response.data;
  }
};
