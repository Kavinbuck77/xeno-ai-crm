import api from './api';

export const campaignApi = {
  getAll: async () => {
    const response = await api.get('/campaigns');
    return response.data;
  },
  getById: async (id) => {
    const response = await api.get(`/campaigns/${id}`);
    return response.data;
  },
  create: async (campaignData) => {
    const response = await api.post('/campaigns', campaignData);
    return response.data;
  },
  generateStrategy: async (goal) => {
    const response = await api.post('/ai/generate-campaign', { goal });
    return response.data;
  },
  launch: async (id) => {
    const response = await api.post(`/campaigns/${id}/launch`);
    return response.data;
  },
  getAnalytics: async (id) => {
    const response = await api.get(`/campaigns/${id}/analytics`);
    return response.data;
  },
  getRecipients: async (id) => {
    const response = await api.get(`/campaigns/${id}/recipients`);
    return response.data;
  }
};
