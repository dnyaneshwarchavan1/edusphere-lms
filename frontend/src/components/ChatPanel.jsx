import { useEffect, useMemo, useRef, useState } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';

function chatSocketUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  return apiBase.replace(/\/api\/?$/, '').replace(/^http/, 'ws') + '/ws/chat';
}

function playIncomingSound() {
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gain = audioContext.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.value = 880;
    gain.gain.setValueAtTime(0.001, audioContext.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.08, audioContext.currentTime + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + 0.18);
    oscillator.connect(gain);
    gain.connect(audioContext.destination);
    oscillator.start();
    oscillator.stop(audioContext.currentTime + 0.18);
  } catch {
    // Ignore audio failures.
  }
}

export default function ChatPanel({ title = 'Live Chat' }) {
  const { user } = useAuth();
  const [contacts, setContacts] = useState([]);
  const [selectedContactId, setSelectedContactId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [chatError, setChatError] = useState('');
  const [connected, setConnected] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    if (!user) return;

    api.get('/chat/bootstrap')
      .then((res) => {
        setContacts(res.data.contacts || []);
        if (!selectedContactId && res.data.contacts?.length) {
          setSelectedContactId(res.data.contacts[0].userId);
        }
      })
      .catch((error) => setChatError(error.response?.data?.message || 'Unable to load chat contacts.'));
  }, [user]);

  useEffect(() => {
    if (!selectedContactId) {
      setMessages([]);
      return;
    }
    api.get(`/chat/messages/${selectedContactId}`)
      .then((res) => setMessages(res.data))
      .catch((error) => setChatError(error.response?.data?.message || 'Unable to load chat messages.'));
  }, [selectedContactId]);

  useEffect(() => {
    const token = localStorage.getItem('edusphere_token');
    if (!user || !token) return;

    const socket = new WebSocket(`${chatSocketUrl()}?token=${encodeURIComponent(token)}`);
    socket.onopen = () => setConnected(true);
    socket.onclose = () => setConnected(false);
    socket.onerror = () => setConnected(false);
    socket.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data);
        if (envelope.type === 'MESSAGE' && envelope.message) {
          setMessages((current) => {
            const message = envelope.message;
            const matchesOpenConversation =
              selectedContactId &&
              (message.senderId === selectedContactId || message.recipientId === selectedContactId);
            if (!matchesOpenConversation) {
              return current;
            }
            if (current.some((item) => item.id === message.id)) {
              return current;
            }
            return [...current, message];
          });
          setContacts((current) => current.map((contact) =>
            contact.userId === envelope.message.senderId || contact.userId === envelope.message.recipientId
              ? { ...contact }
              : contact
          ));
          if (!envelope.message.ownMessage) {
            playIncomingSound();
          }
        }
        if (envelope.type === 'PRESENCE' && envelope.presence) {
          setContacts((current) => current.map((contact) =>
            contact.userId === envelope.presence.userId
              ? { ...contact, online: envelope.presence.online }
              : contact
          ));
        }
      } catch {
        // Ignore malformed messages.
      }
    };

    return () => socket.close();
  }, [selectedContactId, user]);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  async function sendMessage(e) {
    e.preventDefault();
    if (!selectedContactId || !draft.trim()) return;
    setChatError('');
    try {
      await api.post('/chat/messages', {
        recipientId: selectedContactId,
        content: draft.trim()
      });
      setDraft('');
    } catch (error) {
      setChatError(error.response?.data?.message || 'Unable to send message.');
    }
  }

  const selectedContact = useMemo(
    () => contacts.find((contact) => contact.userId === selectedContactId) || null,
    [contacts, selectedContactId]
  );

  const roleThemeClass = user?.role === 'ADMIN'
    ? 'chat-theme-admin'
    : user?.role === 'INSTRUCTOR'
      ? 'chat-theme-instructor'
      : 'chat-theme-student';

  return (
    <div className={`panel chat-panel ${roleThemeClass}`}>
      <div className="d-flex justify-content-between align-items-center gap-3">
        <div>
          <h2 className="h5 mb-1">{title}</h2>
          <p className="text-muted small mb-0">Chat </p>
        </div>
        <span className={connected ? 'socket-live' : 'socket-idle'}>{connected ? 'Live' : 'Offline'}</span>
      </div>
      {chatError && <div className="alert alert-danger mt-3 mb-0">{chatError}</div>}

      <div className="row g-3 mt-1">
        <div className="col-lg-4">
          <div className="chat-contact-list">
            {contacts.length === 0 && <p className="text-muted mb-0">No chat contacts available yet.</p>}
            {contacts.map((contact) => (
              <button
                type="button"
                key={contact.userId}
                className={`chat-contact ${selectedContactId === contact.userId ? 'chat-contact-active' : ''}`}
                onClick={() => setSelectedContactId(contact.userId)}
              >
                <div>
                  <strong>{contact.name}</strong>
                  <div className="small text-muted">{contact.role}</div>
                </div>
                <span className={`chat-status-dot ${contact.online ? 'chat-status-online' : 'chat-status-offline'}`} />
              </button>
            ))}
          </div>
        </div>

        <div className="col-lg-8">
          <div className="chat-window">
            <div className="chat-window-head">
              <strong>{selectedContact ? selectedContact.name : 'Select a contact'}</strong>
              {selectedContact && <span className="small text-muted">{selectedContact.online ? 'Online' : 'Offline'}</span>}
            </div>
            <div className="chat-message-list" ref={listRef}>
              {messages.length === 0 && <p className="text-muted mb-0">No messages yet. Start the conversation.</p>}
              {messages.map((message) => (
                <div key={message.id} className={`chat-bubble-row ${message.ownMessage ? 'chat-bubble-row-own' : ''}`}>
                  <div className={`chat-bubble ${message.ownMessage ? 'chat-bubble-own' : 'chat-bubble-peer'}`}>
                    <div className="small fw-semibold mb-1">{message.ownMessage ? 'You' : message.senderName}</div>
                    <div>{message.content}</div>
                    <div className="chat-time">{new Date(message.createdAt).toLocaleString()}</div>
                  </div>
                </div>
              ))}
            </div>
            <form className="chat-compose" onSubmit={sendMessage}>
              <textarea
                className="form-control"
                rows="3"
                placeholder={selectedContact ? `Message ${selectedContact.name}` : 'Select a contact to start chatting'}
                value={draft}
                disabled={!selectedContact}
                onChange={(e) => setDraft(e.target.value)}
              />
              <button className="btn btn-primary" disabled={!selectedContact || !draft.trim()}>Send</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
