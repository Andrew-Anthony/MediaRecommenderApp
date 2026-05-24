import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { ChatMessage, ChatResponse, Movie } from '../types';
import MovieCard from '../components/MovieCard';
import './Chat.css';

interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
  movies?: Movie[];
}

interface RatingSummary {
  review?: string | null;
}

const STORAGE_KEY = 'chat-turns';
const MIN_RATINGS = 10;

function loadTurns(): ChatTurn[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ChatTurn[]) : [];
  } catch {
    return [];
  }
}

export default function ChatPage() {
  const { userId } = useAuth();
  const [turns, setTurns] = useState<ChatTurn[]>(loadTurns);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [ratingsCount, setRatingsCount] = useState<number | null>(null);
  const [reviewsCount, setReviewsCount] = useState<number | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!userId) return;
    api.get<RatingSummary[]>(`/users/${userId}/ratings`)
      .then((res) => {
        setRatingsCount(res.data.length);
        setReviewsCount(res.data.filter((r) => r.review && r.review.trim().length > 0).length);
      })
      .catch((err) => console.error('Failed to fetch ratings count:', err));
  }, [userId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [turns, loading]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(turns));
  }, [turns]);

  const clearConversation = () => {
    setTurns([]);
    localStorage.removeItem(STORAGE_KEY);
  };

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

  if (ratingsCount === null) {
    return (
      <div className="chat-page">
        <div className="chat-locked">
          <p style={{ color: 'var(--text-muted)' }}>Se încarcă...</p>
        </div>
      </div>
    );
  }

  if (ratingsCount < MIN_RATINGS) {
    const progress = Math.round((ratingsCount / MIN_RATINGS) * 100);
    return (
      <div className="chat-page">
        <div className="chat-locked">
          <div className="chat-locked-icon">🔒</div>
          <h2>Chatbot blocat</h2>
          <p>
            Pentru a folosi recomandările conversaționale, ai nevoie de cel puțin{' '}
            <strong>{MIN_RATINGS} rating-uri</strong> ca să-ți construim un profil relevant.
          </p>
          <div className="chat-locked-progress">
            <div className="chat-locked-progress-bar" style={{ width: `${progress}%` }} />
          </div>
          <p className="chat-locked-progress-text">
            {ratingsCount} / {MIN_RATINGS} rating-uri
          </p>
          <div className="chat-locked-tip">
            <strong>💡 Pentru cele mai bune rezultate:</strong> scrie și review-uri text pe filmele tale preferate.
            Profilul tău devine mult mai precis când chatbot-ul înțelege <em>de ce</em> îți plac filmele,
            nu doar <em>ce</em> îți place.
            {reviewsCount !== null && reviewsCount > 0 && (
              <div style={{ marginTop: 8, color: 'var(--text-muted)' }}>
                Momentan ai <strong>{reviewsCount}</strong> review-uri scrise.
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="chat-page">
      {turns.length > 0 && (
        <button className="chat-clear" onClick={clearConversation}>
          Conversație nouă
        </button>
      )}
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

      {turns.length === 0 && reviewsCount !== null && (
        <div className="chat-tip-subtle">
          💡 <strong>Tip:</strong> chatbot-ul devine mai precis dacă scrii review-uri text pe filmele preferate.
          {reviewsCount > 0 && ` Ai ${reviewsCount} review-${reviewsCount === 1 ? '' : 'uri'} scrise până acum.`}
        </div>
      )}

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
