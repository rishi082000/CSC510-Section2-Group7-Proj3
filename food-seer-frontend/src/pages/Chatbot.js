import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { sendChatMessage, getCurrentUser, getAllFoods } from '../services/api';

const Chatbot = () => {
  const navigate = useNavigate();
  const messagesEndRef = useRef(null);
  const [currentUserId, setCurrentUserId] = useState(null);

  const QUESTIONS = [
    "Hi! I'm your FoodSeer assistant. How are you feeling today? (e.g., tired, energetic, stressed, happy)",
    "How hungry are you right now? (e.g., very hungry, a bit peckish, just want a snack)",
    "What kind of food are you in the mood for? (e.g., something light, comfort food, healthy, sweet)"
  ];

  const loadState = (userId) => {
    if (!userId) return null;
    try {
      const saved = localStorage.getItem(`chatbotState_${userId}`);
      if (saved) {
        const parsed = JSON.parse(saved);
        return {
          messages: parsed.messages || [],
          conversationStep: parsed.conversationStep || 0,
          userResponses: parsed.userResponses || { mood: '', hunger: '', preference: '' },
          recommendedFoods: parsed.recommendedFoods || []
        };
      }
    } catch (error) {
      console.error('Error loading chatbot state:', error);
    }
    return null;
  };

  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [conversationStep, setConversationStep] = useState(0);
  const [userResponses, setUserResponses] = useState({ mood: '', hunger: '', preference: '' });
  const [recommendedFoods, setRecommendedFoods] = useState([]);
  const [stateLoaded, setStateLoaded] = useState(false);

  useEffect(() => {
    const loadUserAndState = async () => {
      try {
        const user = await getCurrentUser();
        setCurrentUserId(user.id);
        const savedState = loadState(user.id);
        if (savedState) {
          setMessages(savedState.messages);
          setConversationStep(savedState.conversationStep);
          setUserResponses(savedState.userResponses);
          setRecommendedFoods(savedState.recommendedFoods);
        }
        setStateLoaded(true);
      } catch (error) {
        console.error('Error loading user:', error);
        navigate('/');
      }
    };
    loadUserAndState();
  }, [navigate]);

  useEffect(() => {
    if (!currentUserId || !stateLoaded) return;
    const state = { messages, conversationStep, userResponses, recommendedFoods };
    localStorage.setItem(`chatbotState_${currentUserId}`, JSON.stringify(state));
  }, [messages, conversationStep, userResponses, recommendedFoods, currentUserId, stateLoaded]);

  useEffect(() => {
    if (messages.length === 0) {
      setMessages([{ role: 'assistant', content: QUESTIONS[0] }]);
    }
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async () => {
    if (!inputMessage.trim()) return;
    const userMessage = { role: 'user', content: inputMessage };
    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const responses = { ...userResponses };
      if (conversationStep === 0) responses.mood = inputMessage;
      if (conversationStep === 1) responses.hunger = inputMessage;
      if (conversationStep === 2) responses.preference = inputMessage;
      setUserResponses(responses);

      if (conversationStep === 2) {
        const aiResponse = await sendChatMessage(inputMessage);
        const foods = await getAllFoods();

        // Filter foods based on AI response
        const matchedFoods = foods.filter(f =>
          aiResponse.message.toLowerCase().includes(f.foodName.toLowerCase())
        );

        if (matchedFoods.length > 0) {
          // Suppress AI response; show only recommendation cards
          const recommendationMessages = matchedFoods.map(f => ({
            role: 'system',
            content: 'recommendation-card',
            food: f
          }));
          setMessages(prev => [...prev, ...recommendationMessages]);
          setRecommendedFoods(matchedFoods); // save all matched foods
        } else {
          // If no foods match, show AI response
          setMessages(prev => [...prev, { role: 'assistant', content: aiResponse.message }]);
        }

      } else {
        const nextStep = conversationStep + 1;
        setConversationStep(nextStep);
        setMessages(prev => [...prev, { role: 'assistant', content: QUESTIONS[nextStep] }]);
      }

    } catch (error) {
      console.error('Error sending message:', error);
      setMessages(prev => [...prev, { role: 'assistant', content: 'Sorry, an error occurred. Please try again.' }]);
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
    const newState = {
      messages: [{ role: 'assistant', content: QUESTIONS[0] }],
      conversationStep: 0,
      userResponses: { mood: '', hunger: '', preference: '' },
      recommendedFoods: []
    };
    setMessages(newState.messages);
    setConversationStep(newState.conversationStep);
    setUserResponses(newState.userResponses);
    setRecommendedFoods(newState.recommendedFoods);

    if (currentUserId) {
      localStorage.setItem(`chatbotState_${currentUserId}`, JSON.stringify(newState));
    }
  };

  return (
    <div className="chatbot-container">
      <div className="chatbot-header">
        <h2>🤖 FoodSeer AI Assistant</h2>
        <p>Let me help you find the perfect meal for your day!</p>
      </div>

      <div className="chatbot-messages">
        {messages.map((msg, index) => (
          <div key={index} className={`message ${msg.role}`}>
            {msg.role === 'system' && msg.content === 'recommendation-card' ? (
              <div className="recommendation-card">
                <h3>🎯 Your Personalized Recommendation</h3>
                <div className="food-card">
                  <h4>{msg.food.foodName}</h4>
                  <p className="food-price">${msg.food.price.toFixed(2)}</p>
                  <p className="food-allergies">
                    {msg.food.allergies && msg.food.allergies.length > 0 ? (
                      <>Contains: {msg.food.allergies.join(', ')}</>
                    ) : (
                      'No common allergens'
                    )}
                  </p>
                  <div className="recommendation-actions">
                    <button onClick={() => handleOrderFood(msg.food)} className="btn-primary">Order This Now!</button>
                    <button onClick={handleStartOver} className="btn-secondary">Get Another Suggestion</button>
                  </div>
                </div>
              </div>
            ) : (
              <>
                <div className="message-avatar">{msg.role === 'user' ? '👤' : '🤖'}</div>
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
          placeholder="Type your answer here..."
          disabled={isLoading || conversationStep > 2}
        />
        <button
          onClick={handleSendMessage}
          disabled={isLoading || !inputMessage.trim() || conversationStep > 2}
          className="btn-send"
        >
          Send
        </button>
      </div>

      <div className="chatbot-footer">
        <button onClick={() => navigate('/recommendations')} className="btn-link">
          Skip to Browse All Foods
        </button>
      </div>
    </div>
  );
};

export default Chatbot;
