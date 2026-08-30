import api from './api';

export const customerApi = {
  getAll: async () => {
    const response = await api.get('/customers');
    return response.data;
  },
  getById: async (id) => {
    const response = await api.get(`/customers/${id}`);
    return response.data;
  },
  create: async (customerData) => {
    const response = await api.post('/customers', customerData);
    return response.data;
  },
  getOrders: async (id) => {
    const response = await api.get(`/customers/${id}/orders`);
    return response.data;
  },
  addOrder: async (orderData) => {
    const response = await api.post('/orders', orderData);
    return response.data;
  }
};
