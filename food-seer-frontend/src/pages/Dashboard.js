import React, { useEffect, useState } from 'react';
import { getAllFoods, getCurrentUser, getUnfulfilledOrders, getFulfilledOrders } from '../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar, Pie, Line } from 'react-chartjs-2';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, PointElement, LineElement, Tooltip, Legend);

const Dashboard = ({ type = 'inventory' }) => {
  const [foods, setFoods] = useState([]);
  const [unfulfilledOrders, setUnfulfilledOrders] = useState([]);
  const [fulfilledOrders, setFulfilledOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        await getCurrentUser(); // Auth check

        if (type === 'inventory') {
          const foodsData = await getAllFoods();
          setFoods(Array.isArray(foodsData) ? foodsData : []);
        } else if (type === 'orders') {
          const unfulfilled = await getUnfulfilledOrders();
          const fulfilled = await getFulfilledOrders();
          setUnfulfilledOrders(unfulfilled);
          setFulfilledOrders(fulfilled);
        }
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [type]);

  if (loading) return <div>Loading dashboard...</div>;

  // -------------------- INVENTORY DASHBOARD --------------------
  if (type === 'inventory') {
    const normalizedFoods = foods.map(f => ({
      id: f.id,
      name: f.foodName,
      price: f.price,
      stock: f.amount,
      rating: f.rating || 0,
      numberOfRatings: f.numberOfRatings || 0,
      allergies: f.allergies || [],
    }));

    // ---- Allergy Breakdown ----
    const allergyCounts = {};
    normalizedFoods.forEach(food => {
      if (food.allergies.length > 0) {
        food.allergies.forEach(allergen => {
          allergyCounts[allergen] = (allergyCounts[allergen] || 0) + 1;
        });
      }
    });

    const allergyLabels = Object.keys(allergyCounts);
    const allergyValues = Object.values(allergyCounts);
    const generateColor = (index) => {
      const palette = [
        '#FF6384','#36A2EB','#FFCE56','#4BC0C0','#9966FF','#FF9F40','#8AC926','#FF6B6B',
        '#00B8A9','#F6416C','#FFB400','#6A0572','#3A86FF','#FF006E'
      ];
      return palette[index % palette.length];
    };
    const allergyColors = allergyLabels.map((_, i) => generateColor(i));
    const allergyData = { labels: allergyLabels, datasets: [{ label: 'Allergies', data: allergyValues, backgroundColor: allergyColors }] };

    // ---- Top Rated Foods ----
    const topRatedFoods = [...normalizedFoods].filter(f => f.rating > 0).sort((a,b)=>b.rating-a.rating).slice(0,8);
    const topRatedData = { labels: topRatedFoods.map(f => f.name), datasets: [{ label: 'Rating', data: topRatedFoods.map(f => f.rating), backgroundColor: 'rgba(54,162,235,0.8)' }] };

    // ---- Stock Levels Chart ----
    const stockData = { 
      labels: normalizedFoods.map(f => f.name), 
      datasets: [{ label: 'Stock', data: normalizedFoods.map(f=>f.stock), backgroundColor: normalizedFoods.map(f=>f.stock<13?'rgba(255,99,132,0.8)':'rgba(75,192,192,0.8)') }] 
    };

    // ---- Stock Value Chart ----
    const stockValueData = { 
      labels: normalizedFoods.map(f => f.name),
      datasets: [{ label: 'Stock Value ($)', data: normalizedFoods.map(f=>f.price*f.stock), backgroundColor:'rgba(153,102,255,0.8)' }]
    };

    const commonOpts = { responsive:true, maintainAspectRatio:false, plugins:{ legend:{position:'top'} }, scales:{ x:{ ticks:{ maxRotation:50, autoSkip:false } } } };

    return (
      <div style={{ padding:16 }}>
        <h2>📊 Inventory Dashboard</h2>
        <div className="dashboard-grid" style={{ display: 'grid', gridTemplateColumns:'repeat(auto-fit, minmax(360px, 1fr))', gap:'40px' }}>
          <div className="chart-container"><h4>Stock Levels</h4><div style={{height:360}}><Bar data={stockData} options={commonOpts} /></div></div>
          <div className="chart-container"><h4>Top Rated Foods</h4><div style={{height:360}}><Bar data={topRatedData} options={commonOpts} /></div></div>
          <div className="chart-container"><h4>Allergy Breakdown</h4><div style={{height:360}}><Pie data={allergyData} options={commonOpts} /></div></div>
          <div className="chart-container"><h4>Stock Value ($)</h4><div style={{height:360}}><Bar data={stockValueData} options={commonOpts} /></div></div>
        </div>
      </div>
    );
  }

  // -------------------- ORDERS DASHBOARD --------------------
  // Inside Dashboard.js — only orders section is updated
// Inside Dashboard.js — only orders section updated
// Inside Dashboard.js — orders dashboard section
// Inside Dashboard.js — orders dashboard section
if (type === 'orders') {
  const allOrders = [...unfulfilledOrders, ...fulfilledOrders];
  const pendingCount = unfulfilledOrders.length;
  const fulfilledCount = fulfilledOrders.length;

  // Aggregate most ordered items
  const itemCounts = {};
  allOrders.forEach(order => {
    order.foods.forEach(food => {
      const name = food.foodName;
      itemCounts[name] = (itemCounts[name] || 0) + 1;
    });
  });

  const sortedItems = Object.entries(itemCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8);

  const mostOrderedLabels = sortedItems.map(([name]) => name);
  const mostOrderedValues = sortedItems.map(([_, count]) => count);

  const mostOrderedData = {
    labels: mostOrderedLabels,
    datasets: [
      {
        label: 'Number of Orders',
        data: mostOrderedValues,
        backgroundColor: 'rgba(156, 39, 176, 0.8)',
      },
    ],
  };

  // Revenue per order
  const revenuePerOrder = allOrders.map(o => o.foods.reduce((sum, f) => sum + f.price, 0));

  // Aggregate revenue per item for top items
  const itemRevenue = {};
  allOrders.forEach(order => {
    order.foods.forEach(food => {
      const name = food.foodName;
      itemRevenue[name] = (itemRevenue[name] || 0) + food.price;
    });
  });

  const sortedRevenueItems = Object.entries(itemRevenue)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8);

  const revenueLabels = sortedRevenueItems.map(([name]) => name);
  const revenueValues = sortedRevenueItems.map(([_, revenue]) => revenue);

  const revenueByItemData = {
    labels: revenueLabels,
    datasets: [
      {
        label: 'Revenue ($)',
        data: revenueValues,
        backgroundColor: 'rgba(75, 192, 192, 0.8)',
      },
    ],
  };

  const commonOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' }, tooltip: { mode: 'index', intersect: false } },
    scales: {
      x: { ticks: { maxRotation: 60, minRotation: 45, autoSkip: false } },
      y: { beginAtZero: true },
    },
  };

  return (
    <div className="dashboard-page">
      <h2 style={{ marginBottom: '24px' }}>📊 Orders Dashboard</h2>
      <div className="dashboard-grid">
        <div className="chart-container">
          <h4>Pending vs Fulfilled Orders</h4>
          <Pie
            data={{
              labels: ['Pending', 'Fulfilled'],
              datasets: [{ data: [pendingCount, fulfilledCount], backgroundColor: ['#ff9800', '#4caf50'] }],
            }}
            options={commonOptions}
          />
        </div>

        <div className="chart-container">
          <h4>Total Revenue per Order</h4>
          <Line
            data={{
              labels: allOrders.map(o => `#${o.id}`),
              datasets: [{ label: 'Revenue ($)', data: revenuePerOrder, borderColor: '#3f51b5', fill: false }],
            }}
            options={commonOptions}
          />
        </div>

        <div className="chart-container">
          <h4>Most Ordered Items</h4>
          <Bar data={mostOrderedData} options={commonOptions} />
        </div>

        <div className="chart-container">
          <h4>Top Items by Revenue</h4>
          <Bar data={revenueByItemData} options={commonOptions} />
        </div>
      </div>
    </div>
  );
}
  return null;
};

export default Dashboard;
