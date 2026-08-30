import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { campaignApi } from "../services/campaignApi";

function CampaignDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [campaign, setCampaign] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [recipients, setRecipients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [launching, setLaunching] = useState(false);

  // Polling ref/flag
  const pollingRef = useRef(null);

  useEffect(() => {
    fetchDetails();
    return () => stopPolling();
  }, [id]);

  const fetchDetails = async () => {
    try {
      const camp = await campaignApi.getById(id);
      setCampaign(camp);
      
      const stats = await campaignApi.getAnalytics(id);
      setAnalytics(stats);

      const list = await campaignApi.getRecipients(id);
      setRecipients(list);
      
      setLoading(false);

      // Start polling if campaign is in RUNNING or QUEUED state
      if (camp.status === "RUNNING" || camp.status === "QUEUED") {
        startPolling();
      } else {
        stopPolling();
      }
    } catch (err) {
      console.error(err);
      setError("Failed to fetch campaign details.");
      setLoading(false);
    }
  };

  const startPolling = () => {
    if (pollingRef.current) return;
    pollingRef.current = setInterval(async () => {
      try {
        const camp = await campaignApi.getById(id);
        setCampaign(camp);
        
        const stats = await campaignApi.getAnalytics(id);
        setAnalytics(stats);

        const list = await campaignApi.getRecipients(id);
        setRecipients(list);

        if (camp.status !== "RUNNING" && camp.status !== "QUEUED") {
          stopPolling();
        }
      } catch (err) {
        console.error("Polling error: ", err);
      }
    }, 3000);
  };

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  const handleLaunch = async () => {
    setLaunching(true);
    try {
      await campaignApi.launch(id);
      fetchDetails();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || "Failed to launch campaign.");
    } finally {
      setLaunching(false);
    }
  };

  if (loading) {
    return (
      <div className="skeleton-container" style={{ padding: 40 }}>
        <div className="skeleton skeleton-title"></div>
        <div className="skeleton skeleton-text" style={{ width: "80%" }}></div>
        <div className="skeleton skeleton-text" style={{ width: "60%" }}></div>
      </div>
    );
  }

  if (error || !campaign) {
    return (
      <div className="empty-state">
        <span className="empty-state-icon">⚠️</span>
        <h3 className="empty-state-title">Campaign not found</h3>
        <p className="empty-state-desc">{error || "Access denied or invalid campaign ID."}</p>
        <button className="btn btn-primary" onClick={() => navigate("/campaigns")}>
          Back to Campaigns
        </button>
      </div>
    );
  }

  // Segment Name Normalization
  const getSegmentName = (type) => {
    if (!type) return "All Customers";
    return type.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  };

  // Custom SVG Donut calculations
  const total = analytics?.totalRecipients || 0;
  const delivered = analytics?.delivered || 0;
  const failed = analytics?.failed || 0;
  const queuedOrSent = total - (delivered + failed);

  // SVG parameters
  const size = 160;
  const strokeWidth = 16;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  const pctDelivered = total > 0 ? (delivered / total) * 100 : 0;
  const pctFailed = total > 0 ? (failed / total) * 100 : 0;
  const pctQueued = total > 0 ? (queuedOrSent / total) * 100 : 0;

  const dashDelivered = circumference * (pctDelivered / 100);
  const dashFailed = circumference * (pctFailed / 100);
  const dashQueued = circumference * (pctQueued / 100);

  // Offset accumulators
  let offset = 0;
  const strokeOffsetDelivered = circumference - dashDelivered;
  offset += dashDelivered;
  
  const strokeOffsetFailed = circumference - dashFailed + offset;
  offset += dashFailed;

  const strokeOffsetQueued = circumference - dashQueued + offset;

  return (
    <div className="campaign-details-page">
      <div className="page-header">
        <div>
          <button className="btn btn-secondary btn-sm mb-4" onClick={() => navigate("/campaigns")}>
            ← Back to Campaigns
          </button>
          <h1 className="page-title">{campaign.name}</h1>
          <p className="page-subtitle">Created on {new Date(campaign.createdAt).toLocaleString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true })}</p>
        </div>
        <div>
          {campaign.status === "DRAFT" ? (
            <button className="btn btn-success" onClick={handleLaunch} disabled={launching}>
              {launching ? "Launching..." : "🚀 Launch Campaign Now"}
            </button>
          ) : (
            <span className={`badge ${campaign.status === "RUNNING" || campaign.status === "QUEUED" ? "badge-warning" : "badge-success"}`} style={{ fontSize: 14, padding: "8px 16px" }}>
              {campaign.status}
            </span>
          )}
        </div>
      </div>

      <div className="grid-3">
        {/* Campaign Info Card */}
        <div className="card" style={{ gridColumn: "span 2" }}>
          <h2 className="card-title">Campaign Strategy Details</h2>
          <p className="card-subtitle">AI-generated configuration and text templates</p>

          <div className="detail-grid" style={{ border: "none", paddingBottom: 0 }}>
            <div className="detail-item">
              <span className="detail-label">Audience Segment</span>
              <span className="detail-value">{getSegmentName(campaign.segmentType)}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Communication Channel</span>
              <span className="detail-value">{campaign.channel}</span>
            </div>
            <div className="detail-item" style={{ gridColumn: "span 2", marginTop: 12 }}>
              <span className="detail-label">Marketing Goal Brief</span>
              <span className="detail-value" style={{ fontWeight: 400, color: "var(--muted)" }}>
                {campaign.message ? "Analyzed goal strategy successfully mapped." : "No goal defined."}
              </span>
            </div>
            <div className="detail-item" style={{ gridColumn: "span 2", marginTop: 12 }}>
              <span className="detail-label">Generated Copy Message</span>
              <p className="reco-message" style={{ margin: 0, fontStyle: "normal" }}>{campaign.message}</p>
            </div>
          </div>
        </div>

        {/* Live Timeline Tracker */}
        <div className="card">
          <h2 className="card-title">Delivery Status Timeline</h2>
          <p className="card-subtitle">Asynchronous execution steps</p>

          <div className="timeline">
            <div className="timeline-item">
              <span className="timeline-dot completed"></span>
              <div className="timeline-content">
                <span className="timeline-title">Campaign Proposal Drafted</span>
                <span className="timeline-time">{new Date(campaign.createdAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true })}</span>
              </div>
            </div>

            <div className="timeline-item">
              <span className={`timeline-dot ${campaign.status !== "DRAFT" ? "completed" : "pending"}`}></span>
              <div className="timeline-content">
                <span className="timeline-title">Audience Evaluation & Queue Setup</span>
                {campaign.status !== "DRAFT" && <span className="timeline-time">Completed</span>}
              </div>
            </div>

            <div className="timeline-item">
              <span className={`timeline-dot ${campaign.status === "RUNNING" || campaign.status === "QUEUED" ? "active" : (campaign.status === "SENT" || campaign.status === "COMPLETED" ? "completed" : "pending")}`}></span>
              <div className="timeline-content">
                <span className="timeline-title">Dispatches to Simulator Engine</span>
                {campaign.status === "RUNNING" && <span className="timeline-time" style={{ color: "var(--warning)" }}>Active transmission...</span>}
                {(campaign.status === "SENT" || campaign.status === "COMPLETED") && <span className="timeline-time">Completed dispatches</span>}
              </div>
            </div>

            <div className="timeline-item">
              <span className={`timeline-dot ${campaign.status === "SENT" || campaign.status === "COMPLETED" ? "completed" : "pending"}`}></span>
              <div className="timeline-content">
                <span className="timeline-title">Delivery Loop Finalized</span>
                {(campaign.status === "SENT" || campaign.status === "COMPLETED") && (
                  <span className="timeline-time text-success" style={{ fontWeight: 600 }}>
                    {analytics?.deliveryRate}% Successful
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {campaign.status !== "DRAFT" && analytics && (
        <div className="grid-3" style={{ marginTop: 24 }}>
          {/* Analytics Stats Grid */}
          <div className="card" style={{ gridColumn: "span 2", marginBottom: 0 }}>
            <h2 className="card-title">Real-Time Delivery Analytics</h2>
            <p className="card-subtitle">Real database figures, no calculated open metrics</p>

            <div className="stats-grid" style={{ marginBottom: 0 }}>
              <div className="stat-card" style={{ padding: 16, border: "1px solid var(--border)" }}>
                <div className="stat-info">
                  <span className="stat-label">Total Recipients</span>
                  <span className="stat-value">{analytics.totalRecipients}</span>
                </div>
              </div>
              <div className="stat-card" style={{ padding: 16, border: "1px solid var(--border)" }}>
                <div className="stat-info">
                  <span className="stat-label">Delivered</span>
                  <span className="stat-value text-success">{analytics.delivered}</span>
                </div>
              </div>
              <div className="stat-card" style={{ padding: 16, border: "1px solid var(--border)" }}>
                <div className="stat-info">
                  <span className="stat-label">Failed</span>
                  <span className="stat-value text-danger">{analytics.failed}</span>
                </div>
              </div>
              <div className="stat-card" style={{ padding: 16, border: "1px solid var(--border)" }}>
                <div className="stat-info">
                  <span className="stat-label">Delivery Success</span>
                  <span className="stat-value" style={{ color: "var(--primary)" }}>{analytics.deliveryRate}%</span>
                </div>
              </div>
            </div>

            <div style={{ marginTop: 20, fontSize: 13, color: "var(--muted)", padding: "10px 14px", background: "#f8fafc", borderRadius: "var(--radius)" }}>
              ℹ️ Open rate & click rate metrics are not implemented. Only physical carrier receipt statuses are logged.
            </div>
          </div>

          {/* SVG Donut Chart Card */}
          <div className="card chart-card" style={{ marginBottom: 0 }}>
            <h2 className="card-title">Distribution Ratio</h2>
            
            {total === 0 ? (
              <p style={{ margin: "40px 0", color: "var(--muted)", fontSize: 14 }}>No data logged yet.</p>
            ) : (
              <>
                <div className="chart-wrapper">
                  <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ transform: "rotate(-90deg)" }}>
                    {/* Circle Background */}
                    <circle
                      cx={size / 2}
                      cy={size / 2}
                      r={radius}
                      fill="transparent"
                      stroke="#f1f5f9"
                      strokeWidth={strokeWidth}
                    />
                    
                    {/* Delivered (Green) */}
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

                    {/* Failed (Red) */}
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

                    {/* Queued / Pending (Muted Gray) */}
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
                  
                  {/* Center percentage label */}
                  <div style={{
                    position: "absolute",
                    top: "50%",
                    left: "50%",
                    transform: "translate(-50%, -50%)",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center"
                  }}>
                    <span style={{ fontSize: 22, fontWeight: 700, color: "var(--text)" }}>{Math.round(pctDelivered)}%</span>
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
                      <span>Processing ({queuedOrSent})</span>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Recipients logs table */}
      {campaign.status !== "DRAFT" && (
        <div className="card mt-4">
          <h2 className="card-title">Recipient Customer Dispatch Log</h2>
          <p className="card-subtitle">Real-time status updates and carrier receipts from simulator callbacks.</p>

          {recipients.length === 0 ? (
            <div className="empty-state" style={{ padding: 24 }}>
              <p className="empty-state-desc" style={{ marginBottom: 0 }}>No recipient list resolved.</p>
            </div>
          ) : (
            <div className="table-container">
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Customer Name</th>
                      <th>Email</th>
                      <th>Phone</th>
                      <th>Delivery Status</th>
                      <th>Processed At</th>
                      <th>Error logs</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recipients.map((r) => {
                      const getCommBadge = (status) => {
                        if (status === "DELIVERED") return <span className="badge badge-success">Delivered</span>;
                        if (status === "FAILED") return <span className="badge badge-danger">Failed</span>;
                        if (status === "SENT") return <span className="badge badge-primary">Sent</span>;
                        return <span className="badge badge-warning">Queued</span>;
                      };

                      return (
                        <tr key={r.communicationId}>
                          <td className="font-semibold">{r.customerName || `Customer #${r.customerId}`}</td>
                          <td>{r.customerEmail || "-"}</td>
                          <td>{r.customerPhone || "-"}</td>
                          <td>{getCommBadge(r.status)}</td>
                          <td>{r.deliveredAt ? new Date(r.deliveredAt).toLocaleString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true }) : "-"}</td>
                          <td className="text-danger" style={{ fontSize: 13 }}>{r.errorMessage || "-"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default CampaignDetails;
