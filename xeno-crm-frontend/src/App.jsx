import { Routes, Route, Navigate, useLocation } from "react-router-dom";

import "./App.css";
import Dashboard from "./pages/Dashboard";
import Customers from "./pages/Customers";
import CampaignStudio from "./pages/CampaignStudio";
import CampaignDetails from "./pages/CampaignDetails";
import Analytics from "./pages/Analytics";
import Login from "./pages/Login";
import Register from "./pages/Register";

import Navbar from "./components/Navbar";
import { authApi } from "./services/authApi";

function PrivateRoute({ children }) {
  const authenticated = authApi.isAuthenticated();
  return authenticated ? children : <Navigate to="/login" replace />;
}

function App() {
  const location = useLocation();
  const isAuthPage = location.pathname === "/login" || location.pathname === "/register";

  return (
    <>
      {isAuthPage ? (
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      ) : (
        <div className="app-layout">
          <Navbar />
          <main className="main-content">
            <Routes>
              <Route
                path="/"
                element={
                  <PrivateRoute>
                    <Dashboard />
                  </PrivateRoute>
                }
              />
              <Route
                path="/customers"
                element={
                  <PrivateRoute>
                    <Customers />
                  </PrivateRoute>
                }
              />
              <Route
                path="/campaigns"
                element={
                  <PrivateRoute>
                    <CampaignStudio />
                  </PrivateRoute>
                }
              />
              <Route
                path="/campaigns/:id"
                element={
                  <PrivateRoute>
                    <CampaignDetails />
                  </PrivateRoute>
                }
              />
              <Route
                path="/analytics"
                element={
                  <PrivateRoute>
                    <Analytics />
                  </PrivateRoute>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      )}
    </>
  );
}

export default App;
