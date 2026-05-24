import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import type { ChatMessage, ChatResponse, Movie } from '../types';
import MovieCard from '../components/MovieCard';
import './Chat.css';

interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
  movies?: Movie[];
}

export default function ChatPage() {
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [turns, loading]);

  const sendMessage = async () => {
    const trimmed = input.trim();
    if (!trimmed || loading) return;

    const userTurn: ChatTurn = { role: 'user', content: trimmed };
    const nextTurns = [...turns, userTurn];
    setTurns(nextTurns);
    setInput('');
    setLoading(true);

    try {
      const history: ChatMessage[] = nextTurns.map((t) => ({
        role: t.role,
        content: t.content,
      }));
      const res = await api.post<ChatResponse>('/chat', { messages: history });
      const assistantTurn: ChatTurn = {
        role: 'assistant',
        content: res.data.reply,
        movies: res.data.movies,
      };
      setTurns([...nextTurns, assistantTurn]);
    } catch (err) {
      console.error('Chat request failed:', err);
      setTurns([
        ...nextTurns,
        {
          role: 'assistant',
          content: 'Ups, ceva nu a funcționat. Încearcă din nou.',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="chat-page">
      <div className="chat-messages">
        {turns.length === 0 && !loading && (
          <div className="chat-empty">
            <h2>Salut! 👋</h2>
            <p>
              Întreabă-mă orice despre filme. Folosesc watchlist-ul, rating-urile și
              review-urile tale ca să-ți recomand exact ce ți s-ar potrivi.
            </p>
            <div className="chat-suggestions">
              <button onClick={() => setInput('Recomandă-mi un thriller psihologic')}>
                Recomandă-mi un thriller psihologic
              </button>
              <button onClick={() => setInput('Vreau ceva ca Inception dar mai relaxant')}>
                Vreau ceva ca Inception dar mai relaxant
              </button>
              <button onClick={() => setInput('Ce filme bune au ieșit recent?')}>
                Ce filme bune au ieșit recent?
              </button>
            </div>
          </div>
        )}

        {turns.map((turn, idx) => (
          <div key={idx} className={`chat-turn chat-turn-${turn.role}`}>
            <div className="chat-bubble">{turn.content}</div>
            {turn.movies && turn.movies.length > 0 && (
              <div className="chat-movies">
                {turn.movies.map((m) => (
                  <MovieCard
                    key={m.id}
                    movie={m}
                    onClick={(movie) => navigate(`/movie/${movie.id}`)}
                  />
                ))}
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="chat-turn chat-turn-assistant">
            <div className="chat-bubble chat-bubble-loading">
              <span></span><span></span><span></span>
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      <div className="chat-input-bar">
        <textarea
          className="chat-input"
          placeholder="Scrie un mesaj..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
          disabled={loading}
        />
        <button
          className="chat-send"
          onClick={sendMessage}
          disabled={loading || !input.trim()}
        >
          Trimite
        </button>
      </div>
    </div>
  );
}
