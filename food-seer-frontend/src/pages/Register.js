import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../services/api';

const Register = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const validateForm = () => {
    if (!formData.username || !formData.email || !formData.password || !formData.confirmPassword || !formData.role) {
      setError('All fields are required');
      return false;
    }
    if (formData.username.length < 3) {
      setError('Username must be at least 3 characters long');
      return false;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setError('Please enter a valid email address');
      return false;
    }
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long');
      return false;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return false;
    }
    return true;
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) return;

    setLoading(true);
    try {
      // pass role to backend
      await register(formData.username, formData.email, formData.password, formData.role);
      navigate('/', { state: { message: 'Registration successful! Please login.' } });
    } catch (err) {
      // prefer backend message when available
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1 className="app-title">🍽️ FoodSeer</h1>
          <p className="app-subtitle">Create your account</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input id="username" name="username" type="text" className="form-input"
              value={formData.username} onChange={handleChange} placeholder="Choose a username" autoComplete="username" />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input id="email" name="email" type="email" className="form-input"
              value={formData.email} onChange={handleChange} placeholder="Enter your email" autoComplete="email" />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input id="password" name="password" type="password" className="form-input"
              value={formData.password} onChange={handleChange} placeholder="Create a password (min 6 characters)" autoComplete="new-password" />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" className="form-input"
              value={formData.confirmPassword} onChange={handleChange} placeholder="Re-enter your password" autoComplete="new-password" />
          </div>

          <div className="form-group">
            <label htmlFor="role">Role</label>
            <select id="role" name="role" className="form-input" value={formData.role} onChange={handleChange}>
              <option value="">Select role</option>
              <option value="staff">Staff</option>
              <option value="customer">Customer</option>
              <option value="driver">Driver</option>
            </select>
          </div>

          {error && <div className="error-message">{error}</div>}

          <button type="submit" className="login-button" disabled={loading}>
            {loading ? 'Creating Account...' : 'Register'}
          </button>
        </form>

        <div className="login-footer">
          <p className="register-link">
            Already have an account? <Link to="/">Login here</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;