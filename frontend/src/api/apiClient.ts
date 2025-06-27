import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  withCredentials: true, // wichtig für Cookies (JWT)
});

export default api;
