import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout/Layout';
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import Dashboard from './pages/Dashboard/Dashboard';
import Lifts from './pages/Lifts/Lifts';
import Maintenances from './pages/Maintenances/Maintenances';
import Events from './pages/Events/Events';
import Users from './pages/Users/Users';
import Organizations from './pages/Organizations/Organizations';
import Notifications from './pages/Notifications/Notifications';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <Layout />
              </PrivateRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="lifts" element={<Lifts />} />
            <Route path="maintenances" element={<Maintenances />} />
            <Route path="events" element={<Events />} />
            <Route path="users" element={<Users />} />
            <Route path="organizations" element={<Organizations />} />
            <Route path="notifications" element={<Notifications />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
