import { useEffect, useState } from "react";
import axios from "axios";

const apiUrl = "http://localhost:8080";

function CampaignStudio() {
  const [goal, setGoal] = useState("");
  const [recommendation, setRecommendation] = useState(null);
  const [savedCampaign, setSavedCampaign] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [launching, setLaunching] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    fetchCampaigns();
  }, []);

  const fetchCampaigns = async () => {
    try {
      const response = await axios.get(`${apiUrl}/campaigns`);
      setCampaigns(response.data.reverse());
    } catch (error) {
      console.error(error);
    }
  };

  const generateCampaign = async () => {
    if (!goal.trim()) {
      setStatusMessage("Please enter a campaign goal.");
      return;
    }

    setLoading(true);
    setStatusMessage("");

    try {
      const response = await axios.post(`${apiUrl}/ai/generate-campaign`, {
        goal,
      });

      setRecommendation({
        name: `AI Campaign: ${goal.trim().slice(0, 45)}`,
        ...response.data,
      });
      setSavedCampaign(null);
    } catch (error) {
      console.error(error);
      setStatusMessage("Unable to generate campaign recommendation.");
    } finally {
      setLoading(false);
    }
  };

  const createCampaign = async () => {
    if (!recommendation) {
      return;
    }

    setCreating(true);
    setStatusMessage("");

    try {
      const response = await axios.post(`${apiUrl}/campaigns`, {
        name: recommendation.name,
        channel: recommendation.channel,
        message: recommendation.message,
      });

      setSavedCampaign(response.data);
      setStatusMessage("Campaign created successfully.");
      fetchCampaigns();
    } catch (error) {
      console.error(error);
      setStatusMessage("Failed to create campaign.");
    } finally {
      setCreating(false);
    }
  };

  const launchCampaign = async () => {
    const campaignId = savedCampaign?.id;

    if (!campaignId) {
      setStatusMessage("Create the campaign before launching it.");
      return;
    }

    setLaunching(true);
    setStatusMessage("");

    try {
      await axios.post(`${apiUrl}/campaigns/${campaignId}/launch`);
      setStatusMessage("Campaign launched successfully.");
      fetchCampaigns();
    } catch (error) {
      console.error(error);
      setStatusMessage("Failed to launch campaign.");
    } finally {
      setLaunching(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">AI Campaign Studio</h1>
          <p className="page-subtitle">
            Generate an AI campaign recommendation, save it, then launch it to
            your customer base.
          </p>
        </div>
      </div>

      <div className="grid-2 gap-30">
        <section className="section-card">
          <div className="section-title-group">
            <h2 className="section-title">Campaign Brief</h2>
            <p className="section-subtitle">
              Share your marketing goal and let AI create a campaign concept.
            </p>
          </div>

          <label className="label">Marketing Goal</label>
          <textarea
            className="textarea"
            rows="4"
            value={goal}
            placeholder="Example: Bring back inactive customers"
            onChange={(e) => setGoal(e.target.value)}
          />

          <div className="button-group">
            <button
              className="button button-primary"
              onClick={generateCampaign}
              disabled={loading}
            >
              {loading ? "Generating..." : "Generate AI Campaign"}
            </button>
          </div>

          {statusMessage && (
            <div
              className={`status-message ${statusMessage.includes("Failed") ? "error" : ""}`}
            >
              {statusMessage}
            </div>
          )}
        </section>

        <section className="section-card">
          <div className="section-title-group">
            <h2 className="section-title">Recent Campaigns</h2>
            <p className="section-subtitle">
              Active campaign progress and status from the last launches.
            </p>
          </div>

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Channel</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.slice(0, 6).map((campaign) => (
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {recommendation && (
        <section className="section-card" style={{ marginTop: 28 }}>
          <div className="section-title-group">
            <h2 className="section-title">AI Recommendation</h2>
            <p className="section-subtitle">
              Review the AI-generated campaign structure before saving.
            </p>
          </div>

          <div className="recommendation-list">
            <div className="recommendation-item">
              <span>Segment</span>
              <p>{recommendation.segment}</p>
            </div>
            <div className="recommendation-item">
              <span>Channel</span>
              <p>{recommendation.channel}</p>
            </div>
            <div className="recommendation-item">
              <span>Message</span>
              <p>{recommendation.message}</p>
            </div>
          </div>

          <div className="button-group" style={{ marginTop: 20 }}>
            <button
              className="button button-secondary"
              onClick={createCampaign}
              disabled={creating}
            >
              {creating ? "Saving..." : "Create Campaign"}
            </button>
            <button
              className="button button-success"
              onClick={launchCampaign}
              disabled={launching || !savedCampaign}
            >
              {launching ? "Launching..." : "Launch Campaign"}
            </button>
          </div>

          {savedCampaign && (
            <p className="saved-note">
              Draft saved: <strong>{savedCampaign.name}</strong>
            </p>
          )}
        </section>
      )}
    </div>
  );
}

export default CampaignStudio;
