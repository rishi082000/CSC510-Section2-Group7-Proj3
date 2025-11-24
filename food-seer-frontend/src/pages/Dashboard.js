import React, { useEffect, useState } from 'react';
import { getAllFoods, getCurrentUser } from '../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar, Pie } from 'react-chartjs-2';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

const Dashboard = () => {
  const [foods, setFoods] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchFoods = async () => {
      try {
        await getCurrentUser(); // Auth check
        const foodsData = await getAllFoods();
        setFoods(Array.isArray(foodsData) ? foodsData : []);
      } catch (error) {
        console.error('Error fetching foods:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchFoods();
  }, []);

  if (loading) return <div>Loading dashboard...</div>;

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

  const allergyData = {
    labels: allergyLabels,
    datasets: [
      {
        label: 'Allergies',
        data: allergyValues,
        backgroundColor: allergyColors,
      },
    ],
  };

  // ---- Top Rated Foods ----
  const topRatedFoods = [...normalizedFoods]
    .filter(f => f.rating > 0)
    .sort((a, b) => b.rating - a.rating)
    .slice(0, 8);

  const topRatedData = {
    labels: topRatedFoods.map(f => f.name),
    datasets: [
      {
        label: 'Rating',
        data: topRatedFoods.map(f => f.rating),
        backgroundColor: 'rgba(54,162,235,0.8)',
      },
    ],
  };

  // ---- Stock Levels Chart ----
const stockData = {
  labels: normalizedFoods.map(f => f.name),
  datasets: [
    {
      label: 'Stock',
      data: normalizedFoods.map(f => f.stock),
      backgroundColor: normalizedFoods.map(f =>
        f.stock < 13 ? 'rgba(255,99,132,0.8)' : 'rgba(75,192,192,0.8)' // red if stock < 5, green otherwise
      ),
    },
  ],
};


  // ---- Stock Value Chart (price * stock) ----
  const stockValueData = {
    labels: normalizedFoods.map(f => f.name),
    datasets: [
      {
        label: 'Stock Value ($)',
        data: normalizedFoods.map(f => f.price * f.stock),
        backgroundColor: 'rgba(153,102,255,0.8)',
      },
    ],
  };

  const commonOpts = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { x: { ticks: { maxRotation: 50, autoSkip: false } } },
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>📊 Inventory Dashboard</h2>
      <div className="dashboard-grid">
        {/* Row 1 */}
        <div className="chart-container">
          <h4>Stock Levels</h4>
          <div style={{ height: 360 }}>
            <Bar data={stockData} options={commonOpts} />
          </div>
        </div>

        <div className="chart-container">
          <h4>Top Rated Foods</h4>
          <div style={{ height: 360 }}>
            <Bar data={topRatedData} options={commonOpts} />
          </div>
        </div>

        {/* Row 2 */}
        <div className="chart-container">
          <h4>Allergy Breakdown</h4>
          <div style={{ height: 360 }}>
            <Pie data={allergyData} options={commonOpts} />
          </div>
        </div>

        <div className="chart-container">
          <h4>Stock Value ($)</h4>
          <div style={{ height: 360 }}>
            <Bar data={stockValueData} options={commonOpts} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
