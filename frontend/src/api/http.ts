import axios from "axios";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
});

// 每次發送請求前，自動帶上登入 Token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

export default http;