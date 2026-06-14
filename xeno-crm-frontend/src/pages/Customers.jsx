import { useEffect, useState } from "react";
import axios from "axios";

const apiUrl = "https://xeno-crm-service.onrender.com";

function Customers() {
  const [customers, setCustomers] = useState([]);
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    totalSpent: "",
    lastOrderDate: "",
  });
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
    try {
      const response = await axios.get(`${apiUrl}/customers`);
      setCustomers(response.data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const addCustomer = async (event) => {
    event.preventDefault();

    try {
      await axios.post(`${apiUrl}/customers`, {
        ...form,
        totalSpent: parseFloat(form.totalSpent) || 0,
      });
      setStatusMessage("Customer added successfully.");
      setForm({
        name: "",
        email: "",
        phone: "",
        totalSpent: "",
        lastOrderDate: "",
      });
      fetchCustomers();
    } catch (error) {
      console.error(error);
      setStatusMessage("Unable to add customer.");
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Customers</h1>
          <p className="page-subtitle">
            Add new customers and review the current customer roster.
          </p>
        </div>
      </div>

      <div className="grid-2 gap-30">
        <section className="section-card">
          <div className="section-title-group">
            <h2 className="section-title">Add Customer</h2>
            <p className="section-subtitle">
              Keep your customer list growing with rich profile details.
            </p>
          </div>
          <form onSubmit={addCustomer} className="form-grid">
            <label className="label">
              Name
              <input
                className="input"
                name="name"
                value={form.name}
                onChange={handleChange}
                required
              />
            </label>
            <label className="label">
              Email
              <input
                className="input"
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                required
              />
            </label>
            <label className="label">
              Phone
              <input
                className="input"
                name="phone"
                value={form.phone}
                onChange={handleChange}
              />
            </label>
            <label className="label">
              Total Spent
              <input
                className="input"
                name="totalSpent"
                type="number"
                step="0.01"
                value={form.totalSpent}
                onChange={handleChange}
              />
            </label>
            <label className="label">
              Last Order Date
              <input
                className="input"
                name="lastOrderDate"
                type="date"
                value={form.lastOrderDate}
                onChange={handleChange}
              />
            </label>

            <div
              className="button-group"
              style={{ justifyContent: "flex-start" }}
            >
              <button className="button button-primary" type="submit">
                Add Customer
              </button>
            </div>

            {statusMessage && (
              <div className="status-message">{statusMessage}</div>
            )}
          </form>
        </section>

        <section className="section-card">
          <div className="section-title-group">
            <h2 className="section-title">Customer Table</h2>
            <p className="section-subtitle">
              Search and review top customer details at a glance.
            </p>
          </div>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Total Spent</th>
                  <th>Last Order Date</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((customer) => (
                  <tr key={customer.id}>
                    <td>{customer.name}</td>
                    <td>{customer.email}</td>
                    <td>{customer.phone}</td>
                    <td>{customer.totalSpent?.toFixed(2)}</td>
                    <td>{customer.lastOrderDate || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

export default Customers;
