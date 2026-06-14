import { NavLink } from "react-router-dom";

function Navbar({ theme, toggleTheme }) {
  return (
    <nav className="navbar">
      <div className="brand">XENO AI CRM</div>
      <div className="nav-links">
        <NavLink
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
          to="/"
        >
          Dashboard
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
          to="/customers"
        >
          Customers
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
          to="/campaigns"
        >
          Campaign Studio
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
          to="/analytics"
        >
          Analytics
        </NavLink>
      </div>
      <div className="navbar-actions">
        <button className="theme-toggle" onClick={toggleTheme}>
          {theme === "terra" ? "Classic" : "Terra"}
        </button>
      </div>
    </nav>
  );
}

export default Navbar;
