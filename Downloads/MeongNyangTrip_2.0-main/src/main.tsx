import { createRoot } from "react-dom/client";
import { setupMockApi } from "./app/mocks/mockApi";
import App from "./app/App.tsx";
import "./styles/index.css";

// VITE_USE_MOCK 환경변수가 true일 때만 목킹 활성화
setupMockApi();

createRoot(document.getElementById("root")!).render(<App />);