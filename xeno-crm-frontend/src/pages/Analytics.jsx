import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { campaignApi } from "../services/campaignApi";
import { analyticsApi } from "../services/analyticsApi";

function Analytics() {
  const [campaigns, setCampaigns] = useState([]);
  const [summary, setSummary] = useState(null);
  const [campaignStats, setCampaignStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchAnalyticsData();
  }, []);

  const fetchAnalyticsData = async () => {
    setLoading(true);
    try {
      // 1. Fetch campaigns and summary counts
      const camps = await campaignApi.getAll();
      setCampaigns(camps.reverse());

      const sumData = await analyticsApi.getSummary();
      setSummary(sumData);

      // 2. Fetch specific statistics for each campaign to populate comparison charts and tables
      const statsList = await Promise.all(
        camps.map(async (c) => {
          try {
            const stats = await campaignApi.getAnalytics(c.id);
            return {
              id: c.id,
              name: c.name,
              channel: c.channel,
              status: c.status,
              ...stats
            };
          } catch (e) {
            return {
              id: c.id,
              name: c.name,
              channel: c.channel,
              status: c.status,
              totalRecipients: 0,
              delivered: 0,
              failed: 0,
              deliveryRate: 0.0
            };
          }
        })
      );
      
      setCampaignStats(statsList);
    } catch (err) {
      console.error(err);
      setError("Failed to fetch analytics statistics.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="skeleton-container" style={{ padding: 40 }}>
        <div className="skeleton skeleton-title"></div>
        <div className="skeleton skeleton-text"></div>
        <div className="skeleton skeleton-text"></div>
      </div>
    );
  }

  // 1. Calculations for Chart 1: Overall Delivery Donut Chart
  const totalSent = summary?.totalCommunications || 0;
  const delivered = summary?.deliveredCount || 0;
  const failed = summary?.failedCount || 0;
  const queuedOrSent = totalSent - (delivered + failed);

  const size = 180;
  const strokeWidth = 18;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  const pctDelivered = totalSent > 0 ? (delivered / totalSent) * 100 : 0;
  const pctFailed = totalSent > 0 ? (failed / totalSent) * 100 : 0;
  const pctQueued = totalSent > 0 ? (queuedOrSent / totalSent) * 100 : 0;

  const dashDelivered = circumference * (pctDelivered / 100);
  const dashFailed = circumference * (pctFailed / 100);
  const dashQueued = circumference * (pctQueued / 100);

  let offset = 0;
  const strokeOffsetDelivered = circumference - dashDelivered;
  offset += dashDelivered;
  const strokeOffsetFailed = circumference - dashFailed + offset;
  offset += dashFailed;
  const strokeOffsetQueued = circumference - dashQueued + offset;

  // 2. Calculations for Chart 3: Channel Distribution (Horizontal Bar Chart)
  const channelCounts = campaignStats.reduce((acc, curr) => {
    const ch = curr.channel || "EMAIL";
    acc[ch] = (acc[ch] || 0) + (curr.totalRecipients || 0);
    return acc;
  }, { EMAIL: 0, WHATSAPP: 0, SMS: 0, PUSH: 0 });

  const maxChannelCount = Math.max(...Object.values(channelCounts), 1);

  // 3. Campaign Performance comparison (Vertical Bar Chart for top campaigns)
  const chartCampaigns = campaignStats
    .filter(c => c.totalRecipients > 0)
    .slice(0, 5); // Limit to top 5 campaigns

  const maxCampaignRecipients = Math.max(...chartCampaigns.map(c => c.totalRecipients), 1);

  return (
    <div className="analytics-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Analytics</h1>
          <p className="page-subtitle">Historical campaign performance and delivery analysis based on actual carrier logs.</p>
        </div>
      </div>

      {error && <div className="auth-error">{error}</div>}

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon primary">📊</div>
          <div className="stat-info">
            <span className="stat-label">Total Campaigns</span>
            <span className="stat-value">{campaigns.length}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon primary">📤</div>
          <div className="stat-info">
            <span className="stat-label">Messages Dispatched</span>
            <span className="stat-value">{totalSent}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon success">✅</div>
          <div className="stat-info">
            <span className="stat-label">Delivered Successfully</span>
            <span className="stat-value text-success">{delivered}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon primary">📈</div>
          <div className="stat-info">
            <span className="stat-label">Overall Success Rate</span>
            <span className="stat-value" style={{ color: "var(--primary)" }}>{summary?.deliveryRate || 0}%</span>
          </div>
        </div>
      </div>

      {totalSent === 0 ? (
        <div className="empty-state" style={{ padding: "60px 24px" }}>
          <span className="empty-state-icon">📈</span>
          <h3 className="empty-state-title">No analytics data logged yet</h3>
          <p className="empty-state-desc">Launch a campaign via the Campaign Studio to generate transmission receipts and visual graphs.</p>
          <button className="btn btn-primary" onClick={() => navigate("/campaigns")}>
            Go to Campaign Studio
          </button>
        </div>
      ) : (
        <>
          {/* Charts Grid */}
          <div className="grid-3" style={{ marginBottom: 24 }}>
            {/* Donut Chart: Delivery breakdown */}
            <div className="card chart-card">
              <h3 className="card-title" style={{ fontSize: 16 }}>Overall Delivery Breakdown</h3>
              <p className="card-subtitle" style={{ marginBottom: 8 }}>Dispatched messages distribution</p>

              <div className="chart-wrapper">
                <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ transform: "rotate(-90deg)" }}>
                  <circle cx={size / 2} cy={size / 2} r={radius} fill="transparent" stroke="#f1f5f9" strokeWidth={strokeWidth} />
                  
                  {dashDelivered > 0 && (
                    <circle
                      cx={size / 2}
                      cy={size / 2}
                      r={radius}
                      fill="transparent"
                      stroke="var(--success)"
                      strokeWidth={strokeWidth}
                      strokeDasharray={`${dashDelivered} ${circumference}`}
                      strokeDashoffset={strokeOffsetDelivered}
                      strokeLinecap="round"
                    />
                  )}

                  {dashFailed > 0 && (
                    <circle
                      cx={size / 2}
                      cy={size / 2}
                      r={radius}
                      fill="transparent"
                      stroke="var(--danger)"
                      strokeWidth={strokeWidth}
                      strokeDasharray={`${dashFailed} ${circumference}`}
                      strokeDashoffset={strokeOffsetFailed}
                      strokeLinecap="round"
                    />
                  )}

                  {dashQueued > 0 && (
                    <circle
                      cx={size / 2}
                      cy={size / 2}
                      r={radius}
                      fill="transparent"
                      stroke="#94a3b8"
                      strokeWidth={strokeWidth}
                      strokeDasharray={`${dashQueued} ${circumference}`}
                      strokeDashoffset={strokeOffsetQueued}
                      strokeLinecap="round"
                    />
                  )}
                </svg>
                
                <div style={{
                  position: "absolute",
                  top: "50%",
                  left: "50%",
                  transform: "translate(-50%, -50%)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center"
                }}>
                  <span style={{ fontSize: 24, fontWeight: 700, color: "var(--text)" }}>{Math.round(pctDelivered)}%</span>
                  <span style={{ fontSize: 10, color: "var(--muted)", textTransform: "uppercase", fontWeight: 600 }}>Success</span>
                </div>
              </div>

              <div className="chart-legend">
                <div className="legend-item">
                  <span className="legend-dot" style={{ background: "var(--success)" }}></span>
                  <span>Delivered ({delivered})</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot" style={{ background: "var(--danger)" }}></span>
                  <span>Failed ({failed})</span>
                </div>
                {queuedOrSent > 0 && (
                  <div className="legend-item">
                    <span className="legend-dot" style={{ background: "#94a3b8" }}></span>
                    <span>Pending ({queuedOrSent})</span>
                  </div>
                )}
              </div>
            </div>

            {/* Campaign Comparison Chart (Vertical Bars) */}
            <div className="card chart-card" style={{ gridColumn: "span 2" }}>
              <h3 className="card-title" style={{ fontSize: 16 }}>Top Campaigns Performance</h3>
              <p className="card-subtitle">Volume compared to successfully delivered messages</p>

              {chartCampaigns.length === 0 ? (
                <p style={{ margin: "40px 0", color: "var(--muted)", fontSize: 14 }}>No active campaigns comparison.</p>
              ) : (
                <div style={{ width: "100%", height: 180, display: "flex", alignItems: "flex-end", justifyContent: "space-around", padding: "10px 20px 20px 20px" }}>
                  {chartCampaigns.map(c => {
                    const totalHeight = ((c.totalRecipients || 0) / maxCampaignRecipients) * 140;
                    const successHeight = ((c.delivered || 0) / maxCampaignRecipients) * 140;

                    return (
                      <div key={c.id} style={{ display: "flex", flexDirection: "column", alignItems: "center", width: "16%" }}>
                        {/* Bars stack container */}
                        <div style={{ height: 140, display: "flex", alignItems: "flex-end", gap: 4, width: "100%", justifyContent: "center" }}>
                          {/* Total Volume (Indigo) */}
                          <div 
                            style={{ width: 14, height: totalHeight, background: "var(--primary-light)", border: "1px solid var(--primary)", borderRadius: "4px 4px 0 0" }} 
                            title={`Total Recipients: ${c.totalRecipients}`}
                          />
                          {/* Success Volume (Green) */}
                          <div 
                            style={{ width: 14, height: successHeight, background: "var(--success-bg)", border: "1px solid var(--success)", borderRadius: "4px 4px 0 0" }} 
                            title={`Delivered: ${c.delivered}`}
                          />
                        </div>
                        {/* Campaign title label */}
                        <span 
                          style={{ fontSize: 11, fontWeight: 600, color: "var(--text)", textOverflow: "ellipsis", whiteSpace: "nowrap", overflow: "hidden", width: "100%", textAlign: "center", marginTop: 8 }}
                          title={c.name}
                        >
                          {c.name}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="chart-legend" style={{ marginTop: 8 }}>
                <div className="legend-item">
                  <span className="legend-dot" style={{ background: "var(--primary-light)", border: "1px solid var(--primary)" }}></span>
                  <span>Total Recipients</span>
                </div>
                <div className="legend-item">
                  <span className="legend-dot" style={{ background: "var(--success-bg)", border: "1px solid var(--success)" }}></span>
                  <span>Delivered</span>
                </div>
              </div>
            </div>
          </div>

          <div className="grid-3" style={{ marginBottom: 24 }}>
            {/* Horizontal channel distribution */}
            <div className="card">
              <h3 className="card-title" style={{ fontSize: 16 }}>Dispatches by Channel</h3>
              <p className="card-subtitle" style={{ marginBottom: 16 }}>Distribution counts per carrier channel</p>

              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                {Object.entries(channelCounts).map(([channel, count]) => {
                  const percentWidth = (count / maxChannelCount) * 100;
                  return (
                    <div key={channel} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, fontWeight: 600 }}>
                        <span style={{ color: "var(--text)" }}>{channel}</span>
                        <span style={{ color: "var(--muted)" }}>{count} messages</span>
                      </div>
                      <div style={{ width: "100%", height: 10, background: "#f1f5f9", borderRadius: 999, overflow: "hidden" }}>
                        <div style={{ width: `${percentWidth}%`, height: "100%", background: "var(--primary)", borderRadius: 999 }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Campaign analytics performance directory table */}
            <div className="card" style={{ gridColumn: "span 2" }}>
              <h3 className="card-title" style={{ fontSize: 16 }}>Campaign Performance Summary</h3>
              <p className="card-subtitle" style={{ marginBottom: 16 }}>Actual dispatch data comparison table</p>

              <div className="table-container" style={{ border: "none", boxShadow: "none" }}>
                <div className="table-scroll">
                  <table style={{ minWidth: "100%" }}>
                    <thead>
                      <tr>
                        <th>Campaign</th>
                        <th>Channel</th>
                        <th>Status</th>
                        <th>Sent</th>
                        <th>Delivered</th>
                        <th>Failed</th>
                        <th className="text-right">Success Rate</th>
                      </tr>
                    </thead>
                    <tbody>
                      {campaignStats.map((c) => {
                        const getStatusClass = (status) => {
                          if (status === "RUNNING") return "badge-warning";
                          if (status === "SENT" || status === "COMPLETED") return "badge-success";
                          return "badge-primary";
                        };

                        return (
                          <tr key={c.id}>
                            <td className="font-semibold" style={{ cursor: "pointer", color: "var(--primary)" }} onClick={() => navigate(`/campaigns/${c.id}`)}>
                              {c.name}
                            </td>
                            <td>
                              <span className="badge badge-primary">{c.channel}</span>
                            </td>
                            <td>
                              <span className={`badge ${getStatusClass(c.status)}`}>
                                {c.status}
                              </span>
                            </td>
                            <td className="font-semibold">{c.totalRecipients || 0}</td>
                            <td className="text-success">{c.delivered || 0}</td>
                            <td className="text-danger">{c.failed || 0}</td>
                            <td className="text-right font-semibold" style={{ color: "var(--primary)" }}>
                              {c.totalRecipients > 0 ? `${c.deliveryRate}%` : "-"}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default Analytics;
