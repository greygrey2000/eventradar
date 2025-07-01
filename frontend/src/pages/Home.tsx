import React from "react";
import "../index.css";

const Home = () => {
  return (
    <div className="welcome-container">
      <div className="welcome-card">
        <h1>
          Willkommen bei{" "}
          <span className="brand">Eventradar</span>!
        </h1>
        <p>
          Entdecke spannende Events, vernetze dich mit Gleichgesinnten und erhalte
          persönliche Empfehlungen – alles auf einer modernen Plattform.
        </p>
        <ul className="welcome-features">
          <li>🔍 Events nach Interessen filtern</li>
          <li>⭐ Persönliche Empfehlungen</li>
          <li>👤 Eigenes Profil verwalten</li>
        </ul>
        <p className="welcome-cta">
          Registriere dich jetzt oder logge dich ein, um die besten Events in
          deiner Nähe zu finden!
        </p>
      </div>
    </div>
  );
};

export default Home;