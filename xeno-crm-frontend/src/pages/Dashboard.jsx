import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { campaignApi } from "../services/campaignApi";
import { analyticsApi } from "../services/analyticsApi";

function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [recentCampaigns, setRecentCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [assistantGoal, setAssistantGoal] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const summaryData = await analyticsApi.getSummary();
      setSummary(summaryData);
    } catch (err) {
      console.error("Failed to load analytics summary:", err);
    }

    try {
      const campaigns = await campaignApi.getAll();
      setRecentCampaigns(campaigns.slice(0, 6));
    } catch (err) {
      console.error("Failed to load campaigns:", err);
    }

    setLoading(false);
  };

  const handleLaunchAssistant = (e) => {
    e.preventDefault();
    if (assistantGoal.trim()) {
      // Store goal in localStorage to let CampaignStudio pre-populate it on load
      localStorage.setItem("assistant_prefilled_goal", assistantGoal.trim());
      navigate("/campaigns");
    }
  };

  const getSegmentName = (type) => {
    if (!type) return "All Customers";
    return type.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  };

  return (
    <div className="dashboard-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Real-time status overview of active customer campaigns and delivery receipt performance.</p>
        </div>
      </div>

      {loading ? (
        <div className="skeleton-container" style={{ padding: 40 }}>
          <div className="skeleton skeleton-title"></div>
          <div className="skeleton skeleton-text" style={{ width: "80%" }}></div>
          <div className="skeleton skeleton-text" style={{ width: "65%" }}></div>
        </div>
      ) : (
        <>
          {/* Summary Stats Grid */}
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon primary">👥</div>
              <div className="stat-info">
                <span className="stat-label">Total Customers</span>
                <span className="stat-value">{summary?.totalCustomers || 0}</span>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon primary">✨</div>
              <div className="stat-info">
                <span className="stat-label">Total Campaigns</span>
                <span className="stat-value">{summary?.totalCampaigns || 0}</span>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon primary">📤</div>
              <div className="stat-info">
                <span className="stat-label">Messages Dispatched</span>
                <span className="stat-value">{summary?.totalCommunications || 0}</span>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon success">📈</div>
              <div className="stat-info">
                <span className="stat-label">Overall Delivery Rate</span>
                <span className="stat-value" style={{ color: "var(--primary)" }}>{summary?.deliveryRate || 0}%</span>
              </div>
            </div>
          </div>

          <div className="grid-3">
            {/* Left section: Recent Campaigns table */}
            <div className="card" style={{ gridColumn: "span 2" }}>
              <h2 className="card-title">Recent Campaigns</h2>
              <p className="card-subtitle">Active and historical campaign dispatches</p>

              {recentCampaigns.length === 0 ? (
                <div className="empty-state" style={{ padding: 32 }}>
                  <span className="empty-state-icon">✨</span>
                  <h3 className="empty-state-title">No campaigns yet</h3>
                  <p className="empty-state-desc">Draft or launch your first targeted CRM campaign strategy using the assistant.</p>
                  <button className="btn btn-primary" onClick={() => navigate("/campaigns")}>
                    Create Campaign
                  </button>
                </div>
              ) : (
                <div className="table-container" style={{ border: "none", boxShadow: "none" }}>
                  <div className="table-scroll">
                    <table style={{ minWidth: "100%" }}>
                      <thead>
                        <tr>
                          <th>Campaign</th>
                          <th>Channel</th>
                          <th>Status</th>
                          <th>Created At</th>
                          <th className="text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {recentCampaigns.map((c) => {
                          const getStatusClass = (status) => {
                            if (status === "RUNNING") return "badge-warning";
                            if (status === "SENT" || status === "COMPLETED") return "badge-success";
                            return "badge-primary";
                          };

                          return (
                            <tr key={c.id}>
                              <td className="font-semibold">{c.name}</td>
                              <td>
                                <span className="badge badge-primary">{c.channel}</span>
                              </td>
                              <td>
                                <span className={`badge ${getStatusClass(c.status)}`}>
                                  {c.status}
                                </span>
                              </td>
                              <td>{new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' })}</td>
                              <td className="text-right">
                                <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/campaigns/${c.id}`)}>
                                  Track
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>

            {/* Right section: AI Assistant widget */}
            <div className="card">
              <h2 className="card-title">AI Campaign Assistant</h2>
              <p className="card-subtitle">Compose targeted customer campaigns in seconds</p>

              <form onSubmit={handleLaunchAssistant}>
                <div className="form-group">
                  <label className="form-label" htmlFor="dashboard-assistant-input">Define Marketing Goal</label>
                  <textarea
                    id="dashboard-assistant-input"
                    className="form-control"
                    rows="4"
                    placeholder="e.g. Bring back customers who bought from us over 6 months ago with a 15% discount code."
                    value={assistantGoal}
                    onChange={(e) => setAssistantGoal(e.target.value)}
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary w-full">
                  ✨ Open in Studio
                </button>
              </form>

              <div style={{ marginTop: 20, padding: 14, background: "#f8fafc", border: "1px solid var(--border)", borderRadius: "var(--radius)", fontSize: 13, color: "var(--muted)", lineHeight: 1.5 }}>
                💡 <strong>Try:</strong> <em>&quot;Promote our new premium catalog to customers who have spent over $500 total.&quot;</em>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default Dashboard;
