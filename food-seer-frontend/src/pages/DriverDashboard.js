import React, { useEffect, useState } from "react";
import { fetchDriverDashboard, getCurrentUser, getUnfulfilledOrders, updateOrderStatus } from "../services/api";

const DriverDashboard = () => {
  const [stats, setStats] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        const user = await getCurrentUser();
        console.log("Current User: ", user.username);
        const data = await fetchDriverDashboard(user.username);
        console.log("Dashboard data: ", data);
        const unfulfilledOrders = await getUnfulfilledOrders();
        setOrders(unfulfilledOrders);
        setStats(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const handlePickupOrDeliver = async (orderId) => {
    try {
      // Find the current order
      const currentOrder = orders.find(o => o.id === orderId);

      // If not yet picked up, just update local state (no backend call)
      if (!currentOrder.isPickedUp) {
        setOrders((prev) =>
          prev.map((o) => {
            if (o.id !== orderId) return o;
            return { ...o, isPickedUp: true, status: "PICKED_UP" };
          })
        );
        return;
      }

      console.log("Updating order status for order ID: ", orderId);
      // If already picked up, this is a delivery - update backend
      await updateOrderStatus(orderId);

      // Update local state after successful backend update
      setOrders((prev) =>
        prev.map((o) => {
          if (o.id !== orderId) return o;
          return { ...o, isPickedUp: true, isFulfilled: true, status: "DELIVERED" };
        })
      );

      // Refresh stats after delivery
      const user = await getCurrentUser();
      const updatedStats = await fetchDriverDashboard(user.username);
      setStats(updatedStats);

    } catch (err) {
      console.error("Error updating order status:", err);
      setError("Failed to update order status. Please try again.");
    }
  };

  if (loading) {
    return <div className="dashboard-page">Loading...</div>;
  }

  if (error) {
    return <div className="dashboard-page">{error}</div>;
  }

  return (
    <div className="dashboard-page">
      {/* Header */}
      <div className="dashboard-header">
        <h1>Driver Dashboard</h1>

        <div className="header-actions">
          <button className="nav-button">Go Offline</button>
          <button className="logout-button">Logout</button>
        </div>
      </div>

      {/* Stats */}
      <div className="dashboard-stats">
        <div className="stat-card">
          <h3>Total Deliveries</h3>
          <p className="stat-number">{stats.totalDeliveries}</p>
        </div>

        <div className="stat-card">
          <h3>Today’s Earnings</h3>
          <p className="stat-number">${stats.totalEarning}</p>
        </div>

        <div className="stat-card">
          <h3>Average Rating</h3>
          <p className="stat-number">{stats.averageRating} ⭐</p>
        </div>

        <div className="stat-card">
          <h3>Active Orders</h3>
          <p className="stat-number">{stats.activeOrders}</p>
        </div>
      </div>

      {/* Orders */}
        <div className="orders-container">
          <div className="orders-header">
            <h1>Assigned Orders</h1>
          </div>

          <div className="orders-list">
            {orders.map((order) => (
              <div
                key={order.id}
                className={`order-card ${
                  order.status === "PENDING" ? "pending" : "completed"
                }`}
              >
                <div className="order-header">
                  <div className="order-title">
                    <h3>Order #{order.id}</h3>
                  </div>

                  <span
                    className={`status-badge ${
                      order.isFulfilled === false
                        ? "pending"
                        : "fulfilled"
                    }`}
                  >
                    {order.status}
                  </span>
                </div>

                <div className="order-summary">

                  <div className="summary-item">
                    <span className="label">Earnings</span>
                    <span className="value">${order.earnings}</span>
                  </div>
                </div>

                 {!order.isFulfilled ? (
                    <button
                      className="fulfill-button"
                      onClick={() => handlePickupOrDeliver(order.id)}
                    >
                      {!order.isPickedUp ? "Pick up" : "Mark as Delivered"}
                    </button>
                  ) : (
                    <div className="fulfilled-badge">Completed</div>
                  )}
              </div>
            ))}
          </div>
        </div>
    </div>
  );
};

export default DriverDashboard;