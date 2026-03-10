import axios, {AxiosInstance, AxiosResponse} from 'axios'

const apiClient: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8181',
  headers: { 'Content-Type': 'application/json' },
})

// Normalize errors
apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  (err: unknown) => {
    const message =
      axios.isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message ?? err.message
        : String(err)
    return Promise.reject(new Error(message))
  }
)

export default apiClient