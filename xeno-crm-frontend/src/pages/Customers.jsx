import { useEffect, useState } from "react";
import { customerApi } from "../services/customerApi";

function Customers() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  
  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [customerOrders, setCustomerOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  
  // Form states
  const [customerForm, setCustomerForm] = useState({ name: "", email: "", phone: "" });
  const [orderForm, setOrderForm] = useState({ amount: "", orderDate: new Date().toISOString().split('T')[0] });
  const [formError, setFormError] = useState("");
  const [orderError, setOrderError] = useState("");

  // Search & Filter state
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [sortBy, setSortBy] = useState("name_asc");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
    setLoading(true);
    try {
      const data = await customerApi.getAll();
      setCustomers(data);
      setError("");
    } catch (err) {
      console.error(err);
      if (!err.response) {
        setError("Cannot connect to the server. Please ensure the backend is running.");
      } else if (err.response.status === 401 || err.response.status === 403) {
        setError("Session expired. Redirecting to login...");
      } else {
        setError("Failed to load customer list.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAddCustomer = async (e) => {
    e.preventDefault();
    setFormError("");
    
    if (!customerForm.name.trim() || !customerForm.email.trim()) {
      setFormError("Name and Email are required.");
      return;
    }

    try {
      await customerApi.create({
        ...customerForm,
        totalSpent: 0.0,
        lastOrderDate: null
      });
      setShowAddModal(false);
      setCustomerForm({ name: "", email: "", phone: "" });
      fetchCustomers();
    } catch (err) {
      console.error(err);
      setFormError(err.response?.data?.message || "Failed to create customer.");
    }
  };

  const handleViewCustomer = async (customer) => {
    setSelectedCustomer(customer);
    setCustomerOrders([]);
    setOrdersLoading(true);
    setOrderError("");
    setOrderForm({ amount: "", orderDate: new Date().toISOString().split('T')[0] });

    try {
      const orders = await customerApi.getOrders(customer.id);
      setCustomerOrders(orders.reverse()); // latest first
    } catch (err) {
      console.error(err);
    } finally {
      setOrdersLoading(false);
    }
  };

  const handleAddOrder = async (e) => {
    e.preventDefault();
    setOrderError("");

    if (!orderForm.amount || parseFloat(orderForm.amount) <= 0) {
      setOrderError("Please enter a valid order amount.");
      return;
    }

    try {
      await customerApi.addOrder({
        customerId: selectedCustomer.id,
        amount: parseFloat(orderForm.amount),
        orderDate: orderForm.orderDate
      });
      
      // Refresh customer list & detailed orders list
      const updatedOrders = await customerApi.getOrders(selectedCustomer.id);
      setCustomerOrders(updatedOrders.reverse());
      
      // Re-fetch active customer stats
      const updatedCustomer = await customerApi.getById(selectedCustomer.id);
      setSelectedCustomer(updatedCustomer);

      setOrderForm({ amount: "", orderDate: new Date().toISOString().split('T')[0] });
      
      // Update global list
      fetchCustomers();
    } catch (err) {
      console.error(err);
      setOrderError("Failed to add order record.");
    }
  };

  // JS Filter & Sorting logic
  const filteredCustomers = customers.filter(c => {
    const matchesSearch = 
      c.name.toLowerCase().includes(search.toLowerCase()) || 
      c.email.toLowerCase().includes(search.toLowerCase());
      
    if (!matchesSearch) return false;

    if (filter === "high_value") {
      return (c.totalSpent || 0) >= 500.0;
    }
    if (filter === "recent") {
      if (!c.lastOrderDate) return false;
      const days = (new Date() - new Date(c.lastOrderDate)) / (1000 * 60 * 60 * 24);
      return days <= 30;
    }
    if (filter === "dormant") {
      if (!c.lastOrderDate) return true; // never purchased
      const days = (new Date() - new Date(c.lastOrderDate)) / (1000 * 60 * 60 * 24);
      return days >= 180;
    }
    if (filter === "active") {
      if (!c.lastOrderDate) return false;
      const days = (new Date() - new Date(c.lastOrderDate)) / (1000 * 60 * 60 * 24);
      return days < 180;
    }

    return true;
  });

  const sortedCustomers = [...filteredCustomers].sort((a, b) => {
    if (sortBy === "name_asc") return a.name.localeCompare(b.name);
    if (sortBy === "name_desc") return b.name.localeCompare(a.name);
    if (sortBy === "spent_desc") return (b.totalSpent || 0) - (a.totalSpent || 0);
    if (sortBy === "spent_asc") return (a.totalSpent || 0) - (b.totalSpent || 0);
    if (sortBy === "date_desc") {
      if (!a.lastOrderDate) return 1;
      if (!b.lastOrderDate) return -1;
      return new Date(b.lastOrderDate) - new Date(a.lastOrderDate);
    }
    if (sortBy === "date_asc") {
      if (!a.lastOrderDate) return 1;
      if (!b.lastOrderDate) return -1;
      return new Date(a.lastOrderDate) - new Date(b.lastOrderDate);
    }
    return 0;
  });

  // Pagination
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentCustomers = sortedCustomers.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(sortedCustomers.length / itemsPerPage);

  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
    }
  };

  const getSegmentBadge = (c) => {
    if ((c.totalSpent || 0) >= 500.0) {
      return <span className="badge badge-success">High Value</span>;
    }
    if (!c.lastOrderDate) {
      return <span className="badge badge-warning">Dormant</span>;
    }
    const days = (new Date() - new Date(c.lastOrderDate)) / (1000 * 60 * 60 * 24);
    if (days >= 180) {
      return <span className="badge badge-warning">Dormant</span>;
    }
    if (days <= 30) {
      return <span className="badge badge-primary">Recent</span>;
    }
    return <span className="badge badge-primary">Active</span>;
  };

  return (
    <div className="customers-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Customers</h1>
          <p className="page-subtitle">Manage customer directory profiles, segments, and transaction lists.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
          + Add Customer
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      <div className="card">
        <div className="filters-bar">
          <input
            className="form-control search-input"
            type="text"
            placeholder="Search by Name or Email..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setCurrentPage(1); }}
          />

          <select
            className="form-control filter-select"
            value={filter}
            onChange={(e) => { setFilter(e.target.value); setCurrentPage(1); }}
          >
            <option value="all">All Segments</option>
            <option value="high_value">High Value (&gt;$500)</option>
            <option value="recent">Recent (&lt;30 days)</option>
            <option value="active">Active (&lt;180 days)</option>
            <option value="dormant">Dormant (&gt;180 days)</option>
          </select>

          <select
            className="form-control filter-select"
            value={sortBy}
            onChange={(e) => { setSortBy(e.target.value); setCurrentPage(1); }}
          >
            <option value="name_asc">Name (A-Z)</option>
            <option value="name_desc">Name (Z-A)</option>
            <option value="spent_desc">Spending (High-Low)</option>
            <option value="spent_asc">Spending (Low-High)</option>
            <option value="date_desc">Last Order (Newest)</option>
            <option value="date_asc">Last Order (Oldest)</option>
          </select>
        </div>

        {loading ? (
          <div className="skeleton-container" style={{ padding: 40 }}>
            <div className="skeleton skeleton-title"></div>
            <div className="skeleton skeleton-text"></div>
            <div className="skeleton skeleton-text"></div>
            <div className="skeleton skeleton-text"></div>
          </div>
        ) : currentCustomers.length === 0 ? (
          <div className="empty-state">
            <span className="empty-state-icon">👥</span>
            <h3 className="empty-state-title">No customers found</h3>
            <p className="empty-state-desc">Try clearing search parameters or adding a new customer profile.</p>
          </div>
        ) : (
          <div className="table-container">
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Segment</th>
                    <th>Total Spent</th>
                    <th>Last Order</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {currentCustomers.map((c) => (
                    <tr key={c.id}>
                      <td className="font-semibold">{c.name}</td>
                      <td>{c.email}</td>
                      <td>{c.phone || "-"}</td>
                      <td>{getSegmentBadge(c)}</td>
                      <td className="font-semibold">${c.totalSpent?.toFixed(2) || "0.00"}</td>
                      <td>{c.lastOrderDate ? new Date(c.lastOrderDate).toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : "Never"}</td>
                      <td className="text-right">
                        <button className="btn btn-secondary btn-sm" onClick={() => handleViewCustomer(c)}>
                          View Profile
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="pagination-controls">
                <span>
                  Showing {indexOfFirstItem + 1} to {Math.min(indexOfLastItem, sortedCustomers.length)} of {sortedCustomers.length} customers
                </span>
                <div className="pagination-buttons">
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    Previous
                  </button>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Add Customer Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Add Customer</h3>
              <button className="btn-close" onClick={() => setShowAddModal(false)}>&times;</button>
            </div>
            <form onSubmit={handleAddCustomer}>
              <div className="modal-body">
                {formError && <div className="auth-error" style={{ marginBottom: 16 }}>{formError}</div>}
                
                <div className="form-group">
                  <label className="form-label" htmlFor="new-name">Full Name</label>
                  <input
                    className="form-control"
                    id="new-name"
                    type="text"
                    placeholder="e.g. Alice Smith"
                    value={customerForm.name}
                    onChange={(e) => setCustomerForm(prev => ({ ...prev, name: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="new-email">Email Address</label>
                  <input
                    className="form-control"
                    id="new-email"
                    type="email"
                    placeholder="e.g. alice@example.com"
                    value={customerForm.email}
                    onChange={(e) => setCustomerForm(prev => ({ ...prev, email: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="new-phone">Phone Number (Optional)</label>
                  <input
                    className="form-control"
                    id="new-phone"
                    type="text"
                    placeholder="e.g. +1 (555) 123-4567"
                    value={customerForm.phone}
                    onChange={(e) => setCustomerForm(prev => ({ ...prev, phone: e.target.value }))}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Create Profile
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Customer Profile Details & Orders Modal */}
      {selectedCustomer && (
        <div className="modal-overlay" onClick={() => setSelectedCustomer(null)}>
          <div className="modal-content" style={{ maxWidth: 700 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Customer Profile</h3>
              <button className="btn-close" onClick={() => setSelectedCustomer(null)}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-label">Name</span>
                  <span className="detail-value">{selectedCustomer.name}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Segment Badge</span>
                  <div>{getSegmentBadge(selectedCustomer)}</div>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Email</span>
                  <span className="detail-value">{selectedCustomer.email}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Phone</span>
                  <span className="detail-value">{selectedCustomer.phone || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Total Amount Spent</span>
                  <span className="detail-value text-success">${selectedCustomer.totalSpent?.toFixed(2) || "0.00"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Last Order Recorded</span>
                  <span className="detail-value">
                    {selectedCustomer.lastOrderDate ? new Date(selectedCustomer.lastOrderDate).toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : "Never"}
                  </span>
                </div>
              </div>

              {/* Add Order Form */}
              <div className="card" style={{ padding: 20, marginBottom: 24, background: "#f8fafc" }}>
                <h4 className="card-title" style={{ fontSize: 15, marginBottom: 12 }}>Record New Order</h4>
                {orderError && <div className="auth-error" style={{ padding: 8, fontSize: 12, marginBottom: 12 }}>{orderError}</div>}
                
                <form onSubmit={handleAddOrder} style={{ display: "flex", gap: 16, alignItems: "flex-end", flexWrap: "wrap" }}>
                  <div className="form-group" style={{ flex: 1, minWidth: 140, marginBottom: 0 }}>
                    <label className="form-label" style={{ fontSize: 11, marginBottom: 4 }} htmlFor="order-amount">Amount ($)</label>
                    <input
                      className="form-control"
                      id="order-amount"
                      type="number"
                      step="0.01"
                      placeholder="e.g. 99.99"
                      value={orderForm.amount}
                      onChange={(e) => setOrderForm(prev => ({ ...prev, amount: e.target.value }))}
                      required
                    />
                  </div>

                  <div className="form-group" style={{ flex: 1, minWidth: 140, marginBottom: 0 }}>
                    <label className="form-label" style={{ fontSize: 11, marginBottom: 4 }} htmlFor="order-date">Date</label>
                    <input
                      className="form-control"
                      id="order-date"
                      type="date"
                      value={orderForm.orderDate}
                      onChange={(e) => setOrderForm(prev => ({ ...prev, orderDate: e.target.value }))}
                      required
                    />
                  </div>

                  <button type="submit" className="btn btn-primary" style={{ padding: "10px 16px" }}>
                    Log Purchase
                  </button>
                </form>
              </div>

              {/* Orders History List */}
              <h4 className="card-title" style={{ fontSize: 15, marginBottom: 12 }}>Transaction History</h4>
              {ordersLoading ? (
                <div className="skeleton-container">
                  <div className="skeleton skeleton-text"></div>
                  <div className="skeleton skeleton-text"></div>
                </div>
              ) : customerOrders.length === 0 ? (
                <div className="empty-state" style={{ padding: 24 }}>
                  <p className="empty-state-desc" style={{ fontSize: 13, marginBottom: 0 }}>No transaction logs recorded for this customer profile yet.</p>
                </div>
              ) : (
                <div className="table-container">
                  <table style={{ minWidth: "100%" }}>
                    <thead>
                      <tr>
                        <th>Order ID</th>
                        <th>Purchase Date</th>
                        <th className="text-right">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {customerOrders.map((o) => (
                        <tr key={o.id}>
                          <td>#{o.id}</td>
                          <td>{new Date(o.orderDate).toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' })}</td>
                          <td className="text-right font-semibold text-success">${o.amount?.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={() => setSelectedCustomer(null)}>
                Close Profile
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Customers;
