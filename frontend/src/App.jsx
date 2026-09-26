import { Navigate, Route, Routes } from 'react-router-dom';
import PublicLayout from './components/PublicLayout';
import DashboardLayout from './components/DashboardLayout';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/public/Home';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ForgotPassword from './pages/auth/ForgotPassword';
import ResetPassword from './pages/auth/ResetPassword';
import ForcedPasswordChange from './pages/auth/ForcedPasswordChange';
import StudentDashboard from './pages/student/StudentDashboard';
import ReportIssue from './pages/student/ReportIssue';
import MyIssues from './pages/student/MyIssues';
import StaffDashboard from './pages/staff/StaffDashboard';
import StaffIssues from './pages/staff/StaffIssues';
import AdminDashboard from './pages/admin/AdminDashboard';
import AllIssues from './pages/admin/AllIssues';
import StaffManagement from './pages/admin/StaffManagement';
import Departments from './pages/admin/Departments';
import Categories from './pages/admin/Categories';
import Locations from './pages/admin/Locations';
import Analytics from './pages/admin/Analytics';
import IssueDetail from './pages/shared/IssueDetail';
import Notifications from './pages/shared/Notifications';
import Profile from './pages/shared/Profile';
import NotFound from './pages/shared/NotFound';


const shell = (role) => (
  <ProtectedRoute role={role}>
    <DashboardLayout />
  </ProtectedRoute>
);

export default function App() {
  return (
    <Routes>
      {/* public */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Home />} />
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/change-password" element={<ProtectedRoute><ForcedPasswordChange /></ProtectedRoute>} />

      {/* student */}
      <Route path="/student" element={shell('STUDENT')}>
        <Route index element={<StudentDashboard />} />
        <Route path="report" element={<ReportIssue />} />
        <Route path="issues" element={<MyIssues />} />
        <Route path="issues/:id" element={<IssueDetail />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      {/* staff */}
      <Route path="/staff" element={shell('STAFF')}>
        <Route index element={<StaffDashboard />} />
        <Route path="issues" element={<StaffIssues />} />
        <Route path="issues/:id" element={<IssueDetail />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      {/* admin */}
      <Route path="/admin" element={shell('ADMIN')}>
        <Route index element={<AdminDashboard />} />
        <Route path="issues" element={<AllIssues />} />
        <Route path="issues/:id" element={<IssueDetail />} />
        <Route path="staff" element={<StaffManagement />} />
        <Route path="departments" element={<Departments />} />
        <Route path="categories" element={<Categories />} />
        <Route path="locations" element={<Locations />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
