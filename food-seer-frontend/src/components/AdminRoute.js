import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { getCurrentUser } from '../services/api';

const AdminRoute = ({ children }) => {
  const [isAdmin, setIsAdmin] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const user = await getCurrentUser();
        setIsAdmin(user.role === 'ROLE_ADMIN');
      } catch {
        setIsAdmin(false);
      }
    };
    fetchUser();
  }, []);

  if (isAdmin === null) return null; // or a loader
  return isAdmin ? children : <Navigate to="/" />;
};

export default AdminRoute;
