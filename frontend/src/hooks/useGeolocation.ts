import { useEffect, useState } from "react";
import api from "../api/apiClient";
import useAuth from "../auth/useAuth";

export function useGeolocation() {
  const [latlong, setLatlong] = useState<string | null>(null);
  const { user } = useAuth();

  useEffect(() => {
    if (user && user.location) {
      api.get("/places/geocode", { params: { location: user.location } })
        .then(res => {
          if (res.data && res.data.lat && res.data.lng) {
            setLatlong(`${res.data.lat},${res.data.lng}`);
          } else {
            setLatlong(null);
          }
        })
        .catch(() => setLatlong(null));
    } else if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatlong(`${pos.coords.latitude},${pos.coords.longitude}`);
        },
        () => setLatlong(null),
        { enableHighAccuracy: false, timeout: 5000 }
      );
    }
  }, [user]);

  return latlong;
}
