import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { sendChatMessage, getCurrentUser, getAllFoods } from '../services/api';

const Chatbot = () => {
  const navigate = useNavigate();
  const messagesEndRef = useRef(null);

  const [currentUserId, setCurrentUserId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [recommendedFoods, setRecommendedFoods] = useState([]);
  const [stateLoaded, setStateLoaded] = useState(false);

  // Load saved state
  const loadState = (userId) => {
    if (!userId) return null;
    try {
      const saved = localStorage.getItem(`chatbotState_${userId}`);
      return saved
        ? JSON.parse(saved)
        : null;
    } catch (err) {
      console.error('Error loading state:', err);
      return null;
    }
  };

  // Load user + state
  useEffect(() => {
    const loadUserAndState = async () => {
      try {
        const user = await getCurrentUser();
        setCurrentUserId(user.id);

        const saved = loadState(user.id);
        if (saved && saved.messages) {
          setMessages(saved.messages);
          setRecommendedFoods(saved.recommendedFoods || []);
        } else {
          setMessages([
            {
              role: 'assistant',
              content: "Hi! I'm FoodSeer. Tell me what you're craving."
            }
          ]);
        }

        setStateLoaded(true);
      } catch (err) {
        console.error(err);
        navigate('/');
      }
    };
    loadUserAndState();
  }, [navigate]);

  // Save state
  useEffect(() => {
    if (!currentUserId || !stateLoaded) return;
    const state = { messages, recommendedFoods };
    localStorage.setItem(`chatbotState_${currentUserId}`, JSON.stringify(state));
  }, [messages, recommendedFoods, currentUserId, stateLoaded]);

  // Auto scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Handle chat send
  const handleSendMessage = async () => {
    if (!inputMessage.trim()) return;

    const userMsg = { role: 'user', content: inputMessage };
    setMessages((prev) => [...prev, userMsg]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const aiResponse = await sendChatMessage(inputMessage);
      const foods = await getAllFoods();

      const matchedFoods = foods.filter((f) =>
        aiResponse.message.toLowerCase().includes(f.foodName.toLowerCase())
      );

      if (matchedFoods.length > 0) {
        const cards = matchedFoods.map((f) => ({
          role: 'system',
          content: 'recommendation-card',
          food: f
        }));

        setMessages((prev) => [...prev, ...cards]);
        setRecommendedFoods(matchedFoods);
      } else {
        setMessages((prev) => [
          ...prev,
          { role: 'assistant', content: aiResponse.message }
        ]);
      }
    } catch (err) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'Sorry, something went wrong.' }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleOrderFood = (food) => {
    if (food) {
      navigate('/create-order', { state: { addToCart: food } });
    }
  };

  const handleStartOver = () => {
    const reset = {
      messages: [
        {
          role: 'assistant',
          content: "Hi! I'm FoodSeer. Tell me what you're craving."
        }
      ],
      recommendedFoods: []
    };

    setMessages(reset.messages);
    setRecommendedFoods([]);

    if (currentUserId) {
      localStorage.setItem(`chatbotState_${currentUserId}`, JSON.stringify(reset));
    }
  };

  return (
    <div className="chatbot-container">
      <div className="chatbot-header">
        <h2>🤖 FoodSeer AI Assistant</h2>
        <p>Tell me your mood, cravings, or food style. I'll suggest the best dish.</p>
      </div>

      <div className="chatbot-messages">
        {messages.map((msg, index) => (
          <div key={index} className={`message ${msg.role}`}>
            {msg.role === 'system' && msg.content === 'recommendation-card' ? (
              <div className="recommendation-card">
                <h3>🎯 Recommended for You</h3>
                <div className="food-card">
                  <h4>{msg.food.foodName}</h4>
                  <p className="food-price">${msg.food.price.toFixed(2)}</p>
                  <p className="food-allergies">
                    {msg.food.allergies?.length
                      ? `Contains: ${msg.food.allergies.join(', ')}`
                      : 'No common allergens'}
                  </p>
                  <div className="recommendation-actions">
                    <button
                      onClick={() => handleOrderFood(msg.food)}
                      className="btn-primary"
                    >
                      Order This Now
                    </button>
                    <button onClick={handleStartOver} className="btn-secondary">
                      New Suggestion
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <>
                <div className="message-avatar">
                  {msg.role === 'user' ? '👤' : '🤖'}
                </div>
                <div className="message-content">{msg.content}</div>
              </>
            )}
          </div>
        ))}

        {isLoading && (
          <div className="message assistant">
            <div className="message-avatar">🤖</div>
            <div className="message-content typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="chatbot-input">
        <input
          type="text"
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Tell me what you're craving..."
          disabled={isLoading}
        />
        <button
          onClick={handleSendMessage}
          disabled={isLoading || !inputMessage.trim()}
          className="btn-send"
        >
          Send
        </button>
      </div>

      <div className="chatbot-footer">
        <button
          onClick={() => navigate('/recommendations')}
          className="btn-link"
        >
          Browse All Foods
        </button>
      </div>
    </div>
  );
};

export default Chatbot;
