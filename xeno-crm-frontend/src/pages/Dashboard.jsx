import { useEffect, useState } from "react";
import axios from "axios";

const apiUrl = "https://xeno-crm-service.onrender.com";

function Dashboard() {
  const [customers, setCustomers] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [communications, setCommunications] = useState([]);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [customersRes, campaignsRes, communicationsRes] = await Promise.all(
        [
          axios.get(`${apiUrl}/customers`),
          axios.get(`${apiUrl}/campaigns`),
          axios.get(`${apiUrl}/communications`),
        ],
      );

      setCustomers(customersRes.data);
      setCampaigns(campaignsRes.data);
      setCommunications(communicationsRes.data);
    } catch (error) {
      console.error(error);
    }
  };

  const deliveredCount = communications.filter(
    (item) => item.status === "DELIVERED",
  ).length;
  const messageRate = communications.length
    ? Math.round((deliveredCount / communications.length) * 100)
    : 0;

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">
            Real-time status from campaigns, customers, and delivery
            performance.
          </p>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total Customers</h3>
          <p>{customers.length}</p>
        </div>
        <div className="stat-card">
          <h3>Total Campaigns</h3>
          <p>{campaigns.length}</p>
        </div>
        <div className="stat-card">
          <h3>Messages Sent</h3>
          <p>{communications.length}</p>
        </div>
        <div className="stat-card">
          <h3>Delivery Rate</h3>
          <p>{messageRate}%</p>
        </div>
      </div>

      <section className="section-card" style={{ marginTop: 20 }}>
        <div className="section-title-group">
          <h2 className="section-title">Recent Campaigns</h2>
          <p className="section-subtitle">
            The most recent campaign activity from your AI marketing engine.
          </p>
        </div>

        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Campaign</th>
                <th>Channel</th>
                <th>Status</th>
                <th>Created At</th>
              </tr>
            </thead>
            <tbody>
              {campaigns
                .slice(-6)
                .reverse()
                .map((campaign) => (
                  <tr key={campaign.id}>
                    <td>{campaign.name}</td>
                    <td>{campaign.channel}</td>
                    <td>
                      <span
                        className={`status-pill ${
                          campaign.status.toLowerCase() === "running"
                            ? "running"
                            : campaign.status.toLowerCase() === "draft"
                              ? "draft"
                              : "success"
                        }`}
                      >
                        {campaign.status}
                      </span>
                    </td>
                    <td>
                      {campaign.createdAt
                        ? new Date(campaign.createdAt).toLocaleDateString()
                        : "-"}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default Dashboard;
