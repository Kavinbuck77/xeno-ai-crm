import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { campaignApi } from "../services/campaignApi";

function CampaignStudio() {
  const [goal, setGoal] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  
  // Recommendation state
  const [recommendation, setRecommendation] = useState(null);
  const [savedCampaign, setSavedCampaign] = useState(null);
  
  // Editing state for recommendation fields
  const [editedCampaign, setEditedCampaign] = useState({
    name: "",
    channel: "",
    message: ""
  });
  
  const [campaigns, setCampaigns] = useState([]);
  const [recentLoading, setRecentLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    fetchRecentCampaigns();
    
    const prefilled = localStorage.getItem("assistant_prefilled_goal");
    if (prefilled) {
      setGoal(prefilled);
      localStorage.removeItem("assistant_prefilled_goal");
    }
  }, []);

  const fetchRecentCampaigns = async () => {
    setRecentLoading(true);
    try {
      const data = await campaignApi.getAll();
      setCampaigns(data.reverse()); // newest first
    } catch (err) {
      console.error(err);
    } finally {
      setRecentLoading(false);
    }
  };

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (!goal.trim()) {
      setError("Please enter a campaign marketing goal.");
      return;
    }

    setLoading(true);
    setError("");
    setRecommendation(null);
    setSavedCampaign(null);
    setStatusMessage("");

    try {
      const data = await campaignApi.generateStrategy(goal);
      setRecommendation(data);
      setEditedCampaign({
        name: data.name || `Promo: ${goal.trim().slice(0, 30)}...`,
        channel: data.channel || "EMAIL",
        message: data.message || ""
      });
    } catch (err) {
      console.error(err);
      setError(
        err.response?.data?.message || "AI service is temporarily unavailable. Please retry shortly."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSaveDraft = async () => {
    if (!recommendation) return;
    
    setActionLoading(true);
    setStatusMessage("");
    setError("");

    try {
      const draft = await campaignApi.create({
        name: editedCampaign.name,
        channel: editedCampaign.channel,
        message: editedCampaign.message,
        segmentType: recommendation.segmentType,
        segmentCriteriaJson: JSON.stringify(recommendation.criteria),
        status: "DRAFT"
      });
      setSavedCampaign(draft);
      setStatusMessage("Campaign saved as draft successfully.");
      fetchRecentCampaigns();
    } catch (err) {
      console.error(err);
      setError("Failed to save campaign draft.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleLaunch = async () => {
    setActionLoading(true);
    setStatusMessage("");
    setError("");

    try {
      let campaignId = savedCampaign?.id;

      // If campaign is not saved yet, save it first
      if (!campaignId) {
        const draft = await campaignApi.create({
          name: editedCampaign.name,
          channel: editedCampaign.channel,
          message: editedCampaign.message,
          segmentType: recommendation.segmentType,
          segmentCriteriaJson: JSON.stringify(recommendation.criteria),
          status: "DRAFT"
        });
        campaignId = draft.id;
        setSavedCampaign(draft);
      }

      const launchResult = await campaignApi.launch(campaignId);
      setStatusMessage(`Campaign launched! ${launchResult.recipientCount} messages queued in background.`);
      
      // Redirect to campaign details after short delay
      setTimeout(() => {
        navigate(`/campaigns/${campaignId}`);
      }, 1500);
      
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || "Failed to launch campaign.");
    } finally {
      setActionLoading(false);
    }
  };

  const getSegmentName = (type) => {
    if (!type) return "";
    return type.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  };

  return (
    <div className="campaign-studio-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">AI Campaign Studio</h1>
          <p className="page-subtitle">Write a goal and let AI draft targeted strategies, messages, and segments.</p>
        </div>
      </div>

      <div className="studio-container">
        {/* Left Side: Goal entry */}
        <div className="card">
          <h2 className="card-title">Campaign Brief</h2>
          <p className="card-subtitle">Share your target audience or promo objective, and Gemini will map out the mechanics.</p>
          
          <form onSubmit={handleGenerate}>
            <div className="form-group">
              <label className="form-label" htmlFor="goal-input">Marketing Goal</label>
              <textarea
                id="goal-input"
                className="form-control goal-textarea"
                placeholder="Example: Re-engage customers who haven't ordered in the past 6 months with a 20% discount coupon code."
                value={goal}
                onChange={(e) => setGoal(e.target.value)}
                disabled={loading}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary w-full" disabled={loading}>
              {loading ? "Analyzing goal & drafting..." : "✨ Generate Campaign Proposal"}
            </button>
          </form>

          {error && <div className="auth-error mt-4">{error}</div>}
          {statusMessage && <div className="auth-success mt-4">{statusMessage}</div>}
        </div>

        {/* Right Side: Proposal Preview / Skeletal states */}
        <div>
          {loading && (
            <div className="card">
              <h2 className="card-title">Generating Proposal</h2>
              <div className="skeleton-container" style={{ marginTop: 24 }}>
                <div className="skeleton skeleton-title"></div>
                <div className="skeleton skeleton-text"></div>
                <div className="skeleton skeleton-text" style={{ width: "80%" }}></div>
                <div className="skeleton skeleton-text" style={{ width: "40%" }}></div>
              </div>
            </div>
          )}

          {!loading && !recommendation && (
            <div className="empty-state" style={{ height: "100%", minHeight: 280 }}>
              <span className="empty-state-icon">✨</span>
              <h3 className="empty-state-title">Awaiting Campaign Brief</h3>
              <p className="empty-state-desc">Submit your marketing goal on the left to see estimated audience size and message copies.</p>
            </div>
          )}

          {!loading && recommendation && (
            <div className="card recommendation-card">
              <h2 className="card-title">AI Recommendation Proposal</h2>
              <p className="card-subtitle">Tune the copy and channel before saving drafts or initiating launches.</p>
              
              <div className="form-group">
                <label className="form-label" htmlFor="reco-name">Campaign Name</label>
                <input
                  className="form-control font-semibold"
                  id="reco-name"
                  type="text"
                  value={editedCampaign.name}
                  onChange={(e) => setEditedCampaign(prev => ({ ...prev, name: e.target.value }))}
                />
              </div>

              <div className="reco-grid">
                <div className="reco-item">
                  <span className="reco-label">Target Segment</span>
                  <span className="reco-value">{getSegmentName(recommendation.segmentType)}</span>
                </div>
                <div className="reco-item">
                  <span className="reco-label">Estimated Recipients</span>
                  <span className={`reco-value reco-recipients ${recommendation.recipientCount === 0 ? 'text-danger' : ''}`}>
                    {recommendation.recipientCount} customers
                  </span>
                </div>
                
                {recommendation.recipientCount === 0 && (
                  <div className="reco-item full-width" style={{ marginTop: 8 }}>
                    <div className="auth-error" style={{ margin: 0, padding: '8px 12px', fontSize: 13 }}>
                      ⚠️ No customers match this audience criteria. Launching is disabled for 0 recipients.
                    </div>
                  </div>
                )}
                
                <div className="reco-item full-width">
                  <span className="reco-label">Inactivity/Spend Thresholds</span>
                  <span className="reco-value" style={{ fontSize: 13, color: "var(--muted)" }}>
                    {Object.entries(recommendation.criteria).map(([k, v]) => `${k}: ${v}`).join(", ") || "None"}
                  </span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="reco-channel">Dispatch Channel</label>
                <select
                  className="form-control"
                  id="reco-channel"
                  value={editedCampaign.channel}
                  onChange={(e) => setEditedCampaign(prev => ({ ...prev, channel: e.target.value }))}
                >
                  <option value="EMAIL">Email</option>
                  <option value="WHATSAPP">WhatsApp</option>
                  <option value="SMS">SMS</option>
                  <option value="PUSH">Push Notification</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="reco-message">Message Template</label>
                <textarea
                  className="form-control reco-message"
                  id="reco-message"
                  rows="4"
                  value={editedCampaign.message}
                  onChange={(e) => setEditedCampaign(prev => ({ ...prev, message: e.target.value }))}
                />
              </div>

              <div className="reco-actions">
                <button
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                  onClick={handleSaveDraft}
                  disabled={actionLoading}
                >
                  Save Draft
                </button>
                <button
                  className="btn btn-primary"
                  style={{ flex: 1 }}
                  onClick={handleLaunch}
                  disabled={actionLoading || recommendation.recipientCount === 0}
                >
                  Launch Campaign
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Recent campaigns dashboard list */}
      <div className="card mt-4">
        <h2 className="card-title">Recent Campaigns Directory</h2>
        <p className="card-subtitle">Review status, channel types, and stats of historical and active campaigns.</p>
        
        {recentLoading ? (
          <div className="skeleton-container">
            <div className="skeleton skeleton-text"></div>
            <div className="skeleton skeleton-text"></div>
          </div>
        ) : campaigns.length === 0 ? (
          <div className="empty-state" style={{ padding: 32 }}>
            <p className="empty-state-desc" style={{ marginBottom: 0 }}>No campaigns recorded yet.</p>
          </div>
        ) : (
          <div className="table-container">
            <table style={{ minWidth: "100%" }}>
              <thead>
                <tr>
                  <th>Campaign Name</th>
                  <th>Segment</th>
                  <th>Channel</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th className="text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.slice(0, 5).map((c) => {
                  const getStatusClass = (status) => {
                    if (status === "RUNNING") return "badge-warning";
                    if (status === "SENT" || status === "COMPLETED") return "badge-success";
                    return "badge-primary"; // DRAFT
                  };

                  return (
                    <tr key={c.id}>
                      <td className="font-semibold">{c.name}</td>
                      <td>{getSegmentName(c.segmentType)}</td>
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
                          View Tracking
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default CampaignStudio;
