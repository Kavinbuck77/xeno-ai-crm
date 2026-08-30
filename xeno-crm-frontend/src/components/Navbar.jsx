import { NavLink } from "react-router-dom";
import { authApi } from "../services/authApi";

function Navbar() {
  const username = authApi.getUsername();

  const handleLogout = () => {
    authApi.logout();
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">XENO</div>
      
      <nav className="sidebar-menu">
        <NavLink
          className={({ isActive }) =>
            isActive ? "sidebar-link active" : "sidebar-link"
          }
          to="/"
        >
          <span>📊</span> Dashboard
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "sidebar-link active" : "sidebar-link"
          }
          to="/customers"
        >
          <span>👥</span> Customers
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "sidebar-link active" : "sidebar-link"
          }
          to="/campaigns"
        >
          <span>✨</span> Campaign Studio
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            isActive ? "sidebar-link active" : "sidebar-link"
          }
          to="/analytics"
        >
          <span>📈</span> Analytics
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        {username && (
          <div className="user-profile">
            <div className="user-avatar">
              {username.charAt(0).toUpperCase()}
            </div>
            <div className="user-details">
              <span className="user-name" title={username}>{username}</span>
              <span className="user-role">Administrator</span>
            </div>
          </div>
        )}
        <button className="btn-logout" onClick={handleLogout}>
          <span>🚪</span> Log Out
        </button>
      </div>
    </aside>
  );
}

export default Navbar;
