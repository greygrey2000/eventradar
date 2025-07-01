import React from "react";
import useAuth from "../auth/useAuth";

const Profile: React.FC = () => {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="page-container profile-page">
      <h2 className="profile-title">Mein Profil</h2>
      <div className="profile-card">
        <div className="profile-row">
          <span role="img" aria-label="Name" className="profile-icon">👤</span>
          <span><strong>Name:</strong> {user.name}</span>
        </div>
        <div className="profile-row">
          <span role="img" aria-label="E-Mail" className="profile-icon">📧</span>
          <span><strong>E-Mail:</strong> {user.email}</span>
        </div>
        <div className="profile-row">
          <span role="img" aria-label="Ort" className="profile-icon">📍</span>
          <span><strong>Ort:</strong> {user.location}</span>
        </div>
        <div className="profile-row">
          <span role="img" aria-label="Interessen" className="profile-icon">⭐</span>
          <span><strong>Interessen:</strong> {user.interests && user.interests.length > 0 ? user.interests.join(", ") : "-"}</span>
        </div>
      </div>
    </div>
  );
};

export default Profile;