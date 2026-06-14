import { useEffect, useState } from "react";
import { Routes, Route } from "react-router-dom";

import "./App.css";
import Dashboard from "./pages/Dashboard";
import Customers from "./pages/Customers";
import CampaignStudio from "./pages/CampaignStudio";
import Analytics from "./pages/Analytics";

import Navbar from "./components/Navbar";

function App() {
  const [theme, setTheme] = useState("default");

  useEffect(() => {
    document.body.classList.toggle("theme-terra", theme === "terra");
  }, [theme]);

  const toggleTheme = () => {
    setTheme((current) => (current === "terra" ? "default" : "terra"));
  };

  return (
    <>
      <Navbar theme={theme} toggleTheme={toggleTheme} />

      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/customers" element={<Customers />} />
        <Route path="/campaigns" element={<CampaignStudio />} />
        <Route path="/analytics" element={<Analytics />} />
      </Routes>
    </>
  );
}

export default App;
