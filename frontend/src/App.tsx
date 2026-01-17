import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { LoginPage } from "./pages/Login/LoginPage";
import UsersPage from "./pages/Users/UsersPage";

function RedirectToLoginPreserveQuery() {
    const location = useLocation();
    return <Navigate to={`/login${location.search}`} replace />;
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<RedirectToLoginPreserveQuery />} />

                <Route path="/login" element={<LoginPage />} />
                <Route path="/users" element={<UsersPage />} />

                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
