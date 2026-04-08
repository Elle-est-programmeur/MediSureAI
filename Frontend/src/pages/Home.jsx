import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import ParticleBackground from "../components/ParticleBackground";
import "./Home.css";
export default function Home() {
  const navigate = useNavigate();
  const { darkMode, setDarkMode } = useTheme();

  const handleMagnet = (e) => {
    const button = e.currentTarget;
    const rect = button.getBoundingClientRect();

    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;

    button.style.transform = `translate(${x * 0.2}px, ${y * 0.2}px)`;
  };

  const resetMagnet = (e) => {
    e.currentTarget.style.transform = "translate(0px, 0px)";
  };

  return (
    <div className="home-container">
      <ParticleBackground darkMode={darkMode} />
      <section className="hero">
        <h1>
          AI-Powered Medical <br /> Document Intelligence
        </h1>

        <p>
          Upload, analyze and ask intelligent questions about your
          medical documents instantly using advanced RAG architecture.
        </p>

        <div className="hero-buttons">
          <button
            className="primary large magnetic"
            onMouseMove={handleMagnet}
            onMouseLeave={resetMagnet}
            onClick={() => navigate("/register")}
          >
            Start Free
          </button>

          <button
            className="secondary large magnetic"
            onMouseMove={handleMagnet}
            onMouseLeave={resetMagnet}
            onClick={() => navigate("/login")}
          >
            Login
          </button>
        </div>
      </section>
    </div>
  );
}