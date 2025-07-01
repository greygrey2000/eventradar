// filepath: c:\Users\greys\Documents\eventradar\eventradar\frontend\src\App.tsx
import React from "react";
import { Routes, Route, Link, Navigate } from "react-router-dom";
import Home from "./pages/Home";
import Events from "./pages/Events";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Profile from "./pages/Profile";
import Recommendations from "./pages/Recommendations";
import useAuth from "./auth/useAuth";

const App: React.FC = () => {
  const { user, loading, logout } = useAuth();

  if (loading) return <div className="centered">Lade...</div>;

  return (
    <div>
      <nav className="navbar">
        <div className="nav-left">
          <Link className="nav-logo" to="/">EventRadar</Link>
          <Link className="nav-link" to="/">Home</Link>
          {user && <Link className="nav-link" to="/events">Events</Link>}
          {user && <Link className="nav-link" to="/recommendations">Empfehlungen</Link>}
          {user && <Link className="nav-link" to="/profile">Profil</Link>}
        </div>
        <div className="nav-right">
          {!user && <Link className="nav-btn" to="/login">Login</Link>}
          {!user && <Link className="nav-btn nav-btn-secondary" to="/register">Registrieren</Link>}
          {user && <button className="nav-btn" onClick={logout}>Logout</button>}
        </div>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/events" element={user ? <Events /> : <Navigate to="/login" />} />
        <Route path="/login" element={user ? <Navigate to="/" /> : <Login />} />
        <Route path="/register" element={user ? <Navigate to="/" /> : <Register />} />
        <Route path="/profile" element={user ? <Profile /> : <Navigate to="/login" />} />
        <Route path="/recommendations" element={user ? <Recommendations /> : <Navigate to="/login" />} />
      </Routes>
    </div>
  );
};

export default App;