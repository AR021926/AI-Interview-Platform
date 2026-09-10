import { useEffect, useState } from "react";
import "./App.css";

const API_URL = "http://localhost:8080";

const authInputStyle = {
  width: "100%",
  padding: "14px",
  marginBottom: "15px",
  borderRadius: "10px",
  border: "1px solid var(--auth-input-border)",
  background: "var(--auth-input-bg)",
  color: "var(--auth-input-text)",
  boxSizing: "border-box",
  fontSize: "16px",
};

const authLinkStyle = {
  background: "none",
  border: "none",
  color: "var(--auth-link)",
  cursor: "pointer",
  fontWeight: "bold",
  fontSize: "inherit",
};

function App() {
  // =========================================================
  // THEME
  // =========================================================

  const [theme, setTheme] = useState(
    localStorage.getItem("theme") || "dark"
  );

  useEffect(() => {
    document.documentElement.setAttribute(
      "data-theme",
      theme
    );

    localStorage.setItem("theme", theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((currentTheme) =>
      currentTheme === "dark" ? "light" : "dark"
    );
  };

  // =========================================================
  // AUTH
  // =========================================================

  const [isLoggedIn, setIsLoggedIn] = useState(
    Boolean(localStorage.getItem("userId"))
  );

  const [authMode, setAuthMode] = useState("login");
  const [authName, setAuthName] = useState("");
  const [authEmail, setAuthEmail] = useState("");
  const [authPassword, setAuthPassword] = useState("");
  const [authConfirmPassword, setAuthConfirmPassword] =
    useState("");
  const [authError, setAuthError] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  // =========================================================
  // INTERVIEW STATE
  // =========================================================

  const [interviews, setInterviews] = useState([]);
  const [currentInterviewId, setCurrentInterviewId] =
    useState(null);

  // Practice questions = exactly 10
  const [questions, setQuestions] = useState([]);

  // Live interview questions = exactly 5
  const [interviewQuestions, setInterviewQuestions] =
    useState([]);

  const [loading, setLoading] = useState(true);
  const [started, setStarted] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [viewingResults, setViewingResults] =
    useState(false);

  const [currentQuestionIndex, setCurrentQuestionIndex] =
    useState(0);

  const [answer, setAnswer] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [scores, setScores] = useState([]);
  const [evaluations, setEvaluations] = useState([]);

  const [savedResults, setSavedResults] = useState([]);

  const [error, setError] = useState("");

  // =========================================================
  // THEME BUTTON
  // =========================================================

  const ThemeToggle = () => (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggleTheme}
    >
      {theme === "dark"
        ? "☀️ Light Mode"
        : "🌙 Dark Mode"}
    </button>
  );

  // =========================================================
  // LOGIN / REGISTER
  // =========================================================

  const handleAuth = async (event) => {
    event.preventDefault();

    setAuthError("");

    if (!authEmail.trim()) {
      setAuthError("Email is required.");
      return;
    }

    if (!authPassword.trim()) {
      setAuthError("Password is required.");
      return;
    }

    if (authMode === "register") {
      if (!authName.trim()) {
        setAuthError("Name is required.");
        return;
      }

      if (authPassword.length < 6) {
        setAuthError(
          "Password must be at least 6 characters."
        );
        return;
      }

      if (authPassword !== authConfirmPassword) {
        setAuthError("Passwords do not match.");
        return;
      }
    }

    try {
      setAuthLoading(true);

      const endpoint =
        authMode === "login"
          ? "/api/auth/login"
          : "/api/auth/register";

      const body =
        authMode === "login"
          ? {
              email: authEmail.trim(),
              password: authPassword,
            }
          : {
              name: authName.trim(),
              email: authEmail.trim(),
              password: authPassword,
            };

      const response = await fetch(
        `${API_URL}${endpoint}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(body),
        }
      );

      let data = {};

      try {
        data = await response.json();
      } catch {
        data = {};
      }

      if (!response.ok) {
        throw new Error(
          data.message ||
            data.error ||
            "Authentication failed."
        );
      }

      if (!data.userId) {
        throw new Error(
          "Login succeeded but user ID was not returned by the backend."
        );
      }

      localStorage.setItem(
        "userId",
        String(data.userId)
      );

      localStorage.setItem(
        "userName",
        data.name || authName
      );

      localStorage.setItem(
        "userEmail",
        data.email || authEmail
      );

      setIsLoggedIn(true);

      setAuthName("");
      setAuthEmail("");
      setAuthPassword("");
      setAuthConfirmPassword("");
      setAuthError("");
    } catch (err) {
      console.error(
        "Authentication error:",
        err
      );

      setAuthError(err.message);
    } finally {
      setAuthLoading(false);
    }
  };

  // =========================================================
  // LOGOUT
  // =========================================================

  const handleLogout = () => {
    localStorage.removeItem("userId");
    localStorage.removeItem("userName");
    localStorage.removeItem("userEmail");

    setIsLoggedIn(false);

    setAuthMode("login");
    setAuthName("");
    setAuthEmail("");
    setAuthPassword("");
    setAuthConfirmPassword("");
    setAuthError("");

    setInterviews([]);
    setQuestions([]);
    setInterviewQuestions([]);
    setCurrentInterviewId(null);
    setStarted(false);
    setCompleted(false);
    setViewingResults(false);
  };

  // =========================================================
  // FORMAT DATE + TIME
  // =========================================================

  const formatDateTime = (interview) => {
    const rawDate =
      interview?.createdAt ??
      interview?.created_at ??
      interview?.createdDate ??
      interview?.created_date;

    if (!rawDate) {
      return "Date unavailable";
    }

    const value = String(rawDate);

    /*
      Spring Boot LocalDateTime example:

      2026-09-02T22:32:08.801343

      Read it directly instead of new Date()
      so browser timezone conversion does not change it.
    */

    const match = value.match(
      /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/
    );

    if (!match) {
      return "Date unavailable";
    }

    const [, year, month, day, hour, minute] =
      match;

    const hours = Number(hour);
    const minutes = Number(minute);

    let displayHour = hours % 12;

    if (displayHour === 0) {
      displayHour = 12;
    }

    const amPm = hours >= 12 ? "PM" : "AM";

    const monthNames = [
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec",
    ];

    const monthName =
      monthNames[Number(month) - 1] || month;

    return `${day} ${monthName} ${year}, ${String(
      displayHour
    ).padStart(2, "0")}:${String(minutes).padStart(
      2,
      "0"
    )} ${amPm}`;
  };

  // =========================================================
  // LOAD INTERVIEWS + PRACTICE QUESTIONS
  // =========================================================

  useEffect(() => {
    if (!isLoggedIn) {
      setLoading(false);
      setInterviews([]);
      setQuestions([]);
      return;
    }

    const loadData = async () => {
      const userId = localStorage.getItem("userId");

      if (!userId) {
        setLoading(false);
        setInterviews([]);
        setQuestions([]);
        return;
      }

      try {
        setLoading(true);
        setError("");

        // =====================================================
        // LOAD ONLY THIS USER'S INTERVIEWS
        // =====================================================

        const response = await fetch(
          `${API_URL}/api/interviews?userId=${encodeURIComponent(
            userId
          )}`
        );

        if (!response.ok) {
          throw new Error(
            `Failed to load interviews. HTTP ${response.status}`
          );
        }

        const data = await response.json();

        console.log("Interviews:", data);

        const interviewList = Array.isArray(data)
          ? data
          : [];

        setInterviews(interviewList);

        // =====================================================
        // LOAD 10 PRACTICE QUESTIONS INDEPENDENTLY
        // =====================================================

        const questionResponse = await fetch(
          `${API_URL}/api/questions`
        );

        if (!questionResponse.ok) {
          throw new Error(
            `Failed to load practice questions. HTTP ${questionResponse.status}`
          );
        }

        const questionData =
          await questionResponse.json();

        console.log(
          "Practice questions from backend:",
          questionData
        );

        const allQuestions = Array.isArray(
          questionData
        )
          ? questionData
          : [];

        const practiceQuestions =
          allQuestions.slice(0, 10);

        setQuestions(practiceQuestions);

        console.log(
          "Practice questions (10):",
          practiceQuestions
        );
      } catch (err) {
        console.error(
          "Backend connection error:",
          err
        );

        setError(err.message);
        setInterviews([]);
        setQuestions([]);
        setInterviewQuestions([]);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [isLoggedIn]);

  // =========================================================
  // REFRESH INTERVIEWS
  // =========================================================

  const refreshInterviews = async () => {
    try {
      const userId = localStorage.getItem("userId");

      if (!userId) {
        return null;
      }

      const response = await fetch(
        `${API_URL}/api/interviews?userId=${encodeURIComponent(
          userId
        )}`
      );

      if (!response.ok) {
        throw new Error(
          `Failed to refresh interviews. HTTP ${response.status}`
        );
      }

      const data = await response.json();

      const interviewList = Array.isArray(data)
        ? data
        : [];

      console.log(
        "Refreshed interviews:",
        interviewList
      );

      setInterviews(interviewList);

      return interviewList;
    } catch (err) {
      console.error(
        "Could not refresh interviews:",
        err
      );

      return null;
    }
  };

  // =========================================================
  // START EXISTING INTERVIEW
  // =========================================================

  const startInterview = async (
    interviewId = null
  ) => {
    try {
      let id = interviewId;

      if (id === null) {
        if (interviews.length === 0) {
          alert("No interview available.");
          return;
        }

        id = interviews[0].id;
      }

      const response = await fetch(
        `${API_URL}/api/questions/interview/${id}`
      );

      if (!response.ok) {
        throw new Error(
          `Failed to load questions. HTTP ${response.status}`
        );
      }

      const data = await response.json();

      console.log(
        "All available interview questions:",
        data
      );

      if (!Array.isArray(data) || data.length < 5) {
        alert(
          `This interview has ${
            Array.isArray(data) ? data.length : 0
          } questions. At least 5 questions are required.`
        );

        return;
      }

      const shuffledQuestions = [...data].sort(
        () => Math.random() - 0.5
      );

      const selectedQuestions =
        shuffledQuestions.slice(0, 5);

      console.log(
        "Selected EXACTLY 5 interview questions:",
        selectedQuestions
      );

      if (selectedQuestions.length !== 5) {
        alert(
          "Could not create a 5-question interview."
        );

        return;
      }

      setCurrentInterviewId(id);
      setInterviewQuestions(selectedQuestions);

      setCurrentQuestionIndex(0);
      setAnswer("");

      setScores([]);
      setEvaluations([]);
      setSavedResults([]);

      setCompleted(false);
      setViewingResults(false);
      setStarted(true);
    } catch (err) {
      console.error(
        "Error starting interview:",
        err
      );

      alert(
        `Could not load interview questions.\n\n${err.message}`
      );
    }
  };

  // =========================================================
  // CREATE NEW INTERVIEW
  // =========================================================

  const createNewInterview = async () => {
    try {
      setLoading(true);

      const userId = localStorage.getItem("userId");

      if (!userId) {
        alert(
          "User ID not found. Please login again."
        );

        setIsLoggedIn(false);
        return;
      }

      console.log(
        "Creating new interview for user:",
        userId
      );

      const response = await fetch(
        `${API_URL}/api/interviews`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            title: "Java Developer Interview",
            role: "Software Engineer",
            status: "IN_PROGRESS",
            userId: userId,
          }),
        }
      );

      if (!response.ok) {
        const errorText =
          await response.text();

        throw new Error(
          `Failed to create new interview. HTTP ${response.status}: ${errorText}`
        );
      }

      const newInterview =
        await response.json();

      console.log(
        "New interview created:",
        newInterview
      );

      await refreshInterviews();

      await startInterview(
        newInterview.id
      );
    } catch (err) {
      console.error(
        "New interview error:",
        err
      );

      alert(
        `Could not create new interview.\n\n${err.message}`
      );
    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // DELETE INTERVIEW
  // =========================================================

  const deleteInterview = async (
    interviewId
  ) => {
    const confirmed = window.confirm(
      "Are you sure you want to delete this interview?"
    );

    if (!confirmed) {
      return;
    }

    try {
      const response = await fetch(
        `${API_URL}/api/interviews/${interviewId}`,
        {
          method: "DELETE",
        }
      );

      if (!response.ok) {
        const errorText =
          await response.text();

        throw new Error(
          `Failed to delete interview. HTTP ${response.status}: ${errorText}`
        );
      }

      setInterviews(
        (previousInterviews) =>
          previousInterviews.filter(
            (interview) =>
              interview.id !== interviewId
          )
      );

      if (currentInterviewId === interviewId) {
        resetInterview();
      }

      alert(
        "Interview deleted successfully."
      );
    } catch (err) {
      console.error(
        "Delete interview error:",
        err
      );

      alert(
        `Could not delete interview.\n\n${err.message}`
      );
    }
  };

  // =========================================================
  // VIEW SAVED RESULTS
  // =========================================================

  const viewInterviewResults = async (
    interviewId
  ) => {
    try {
      setLoading(true);

      const refreshedInterviews =
        await refreshInterviews();

      const response = await fetch(
        `${API_URL}/api/interviews/${interviewId}/results`
      );

      if (!response.ok) {
        const errorText =
          await response.text();

        throw new Error(
          `Failed to load interview results. HTTP ${response.status}: ${errorText}`
        );
      }

      const data = await response.json();

      console.log(
        "Saved interview results:",
        data
      );

      setSavedResults(
        Array.isArray(data) ? data : []
      );

      setCurrentInterviewId(interviewId);

      setStarted(false);
      setCompleted(false);
      setViewingResults(true);

      if (refreshedInterviews) {
        const refreshedInterview =
          refreshedInterviews.find(
            (item) =>
              Number(item.id) ===
              Number(interviewId)
          );

        if (refreshedInterview) {
          console.log(
            "Latest saved score:",
            refreshedInterview.finalScore
          );
        }
      }
    } catch (err) {
      console.error(
        "Error loading interview results:",
        err
      );

      alert(
        `Could not load interview results.\n\n${err.message}`
      );
    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // SUBMIT ANSWER
  // =========================================================

  const submitAnswer = async () => {
    if (!answer.trim()) {
      alert(
        "Please enter an answer first."
      );
      return;
    }

    if (interviewQuestions.length !== 5) {
      alert(
        "This interview must contain exactly 5 questions."
      );
      return;
    }

    const currentQuestion =
      interviewQuestions[
        currentQuestionIndex
      ];

    if (!currentQuestion) {
      alert("Question not found.");
      return;
    }

    try {
      setSubmitting(true);

      console.log(
        "Submitting question:",
        currentQuestion.id
      );

      const response = await fetch(
        `${API_URL}/api/answers`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            answerText: answer,
            questionId: currentQuestion.id,
          }),
        }
      );

      if (!response.ok) {
        const errorText =
          await response.text();

        throw new Error(
          `Failed to submit answer. HTTP ${response.status}: ${errorText}`
        );
      }

      const data =
        await response.json();

      console.log(
        "Answer submitted successfully:",
        data
      );

      const score =
        Number(data.score) || 0;

      setScores(
        (previousScores) => [
          ...previousScores,
          score,
        ]
      );

      setEvaluations(
        (previousEvaluations) => [
          ...previousEvaluations,
          {
            question:
              currentQuestion.questionText,
            answer: answer,
            score: score,
            feedback: data.feedback,
          },
        ]
      );

      const nextIndex =
        currentQuestionIndex + 1;

      // =====================================================
      // QUESTION 5 = COMPLETE
      // =====================================================

      if (nextIndex >= 5) {
        console.log(
          "All 5 questions answered."
        );

        const selectedQuestionIds =
          interviewQuestions.map(
            (question) => question.id
          );

        if (
          selectedQuestionIds.length !== 5
        ) {
          throw new Error(
            "Exactly 5 question IDs are required."
          );
        }

        console.log(
          "EXACT 5 QUESTION IDs:",
          selectedQuestionIds
        );

        const completeResponse =
          await fetch(
            `${API_URL}/api/interviews/${currentInterviewId}/complete`,
            {
              method: "POST",
              headers: {
                "Content-Type":
                  "application/json",
              },
              body: JSON.stringify({
                questionIds:
                  selectedQuestionIds,
              }),
            }
          );

        if (!completeResponse.ok) {
          const errorText =
            await completeResponse.text();

          throw new Error(
            `Failed to complete interview. HTTP ${completeResponse.status}: ${errorText}`
          );
        }

        const completedInterview =
          await completeResponse.json();

        console.log(
          "Interview completed:",
          completedInterview
        );

        setInterviews(
          (previousInterviews) =>
            previousInterviews.map(
              (interview) =>
                Number(interview.id) ===
                Number(currentInterviewId)
                  ? {
                      ...interview,
                      status: "COMPLETED",
                      finalScore:
                        completedInterview.finalScore,
                      createdAt:
                        completedInterview.createdAt ??
                        interview.createdAt,
                    }
                  : interview
            )
        );

        await refreshInterviews();

        setAnswer("");
        setStarted(false);
        setCompleted(true);
        setViewingResults(false);

        return;
      }

      // =====================================================
      // NEXT QUESTION
      // =====================================================

      setCurrentQuestionIndex(
        nextIndex
      );

      setAnswer("");
    } catch (err) {
      console.error(
        "Answer submission error:",
        err
      );

      alert(
        `Could not submit answer.\n\n${err.message}`
      );
    } finally {
      setSubmitting(false);
    }
  };

  // =========================================================
  // FINAL SCORE
  // =========================================================

  const getFinalScore = () => {
    if (scores.length === 0) {
      return 0;
    }

    const total = scores.reduce(
      (sum, score) => sum + score,
      0
    );

    return Math.round(
      total / scores.length
    );
  };

  // =========================================================
  // PERFORMANCE
  // =========================================================

  const getPerformanceMessage = () => {
    const score = getFinalScore();

    if (score >= 80) {
      return "Excellent Performance";
    }

    if (score >= 60) {
      return "Good Performance";
    }

    if (score >= 40) {
      return "Needs Improvement";
    }

    return "Needs Significant Improvement";
  };

  const getScoreClass = (score) => {
    const numericScore = Number(score) || 0;

    if (numericScore >= 80) {
      return "score-excellent";
    }

    if (numericScore >= 60) {
      return "score-good";
    }

    if (numericScore >= 40) {
      return "score-needs-improvement";
    }

    return "score-significant-improvement";
  };

  const getScoreLabel = (score) => {
    const numericScore = Number(score) || 0;

    if (numericScore >= 80) {
      return `🟢 ${numericScore}/100 — Excellent`;
    }

    if (numericScore >= 60) {
      return `🔵 ${numericScore}/100 — Good`;
    }

    if (numericScore >= 40) {
      return `🟠 ${numericScore}/100 — Needs Improvement`;
    }

    return `🔴 Below 40 — Needs Significant Improvement`;
  };

  // =========================================================
  // RESET
  // =========================================================

  const resetInterview = () => {
    setCompleted(false);
    setStarted(false);
    setViewingResults(false);

    setCurrentInterviewId(null);
    setCurrentQuestionIndex(0);

    setAnswer("");

    setScores([]);
    setEvaluations([]);
    setSavedResults([]);

    setInterviewQuestions([]);
  };

  // =========================================================
  // LOGIN / REGISTER SCREEN
  // =========================================================

  if (!isLoggedIn) {
    return (
      <div className="app">
        <main
          style={{
            minHeight: "100vh",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            padding: "30px",
            position: "relative",
          }}
        >
          <div
            style={{
              position: "absolute",
              top: "25px",
              right: "25px",
            }}
          >
            <ThemeToggle />
          </div>

          <section
            style={{
              width: "100%",
              maxWidth: "420px",
              padding: "35px",
              borderRadius: "20px",
              background:
                "var(--auth-card-bg)",
              border:
                "1px solid var(--auth-card-border)",
              color: "var(--auth-card-text)",
              boxSizing: "border-box",
            }}
          >
            <div
              style={{
                textAlign: "center",
                marginBottom: "28px",
              }}
            >
              <div className="logo">
                AI<span>Interview</span>
              </div>

              <p
                className="eyebrow"
                style={{
                  marginTop: "25px",
                }}
              >
                AI-POWERED INTERVIEW
                PLATFORM
              </p>

              <h1>
                {authMode === "login"
                  ? "Welcome Back"
                  : "Create Account"}
              </h1>

              <p className="hero-text">
                {authMode === "login"
                  ? "Login to continue your interview practice."
                  : "Register to start your interview practice."}
              </p>
            </div>

            <form onSubmit={handleAuth}>
              {authMode === "register" && (
                <input
                  type="text"
                  placeholder="Name"
                  value={authName}
                  onChange={(e) =>
                    setAuthName(e.target.value)
                  }
                  required
                  style={authInputStyle}
                />
              )}

              <input
                type="email"
                placeholder="Email"
                value={authEmail}
                onChange={(e) =>
                  setAuthEmail(e.target.value)
                }
                required
                style={authInputStyle}
              />

              <input
                type="password"
                placeholder="Password"
                value={authPassword}
                onChange={(e) =>
                  setAuthPassword(e.target.value)
                }
                required
                style={authInputStyle}
              />

              {authMode === "register" && (
                <input
                  type="password"
                  placeholder="Confirm Password"
                  value={authConfirmPassword}
                  onChange={(e) =>
                    setAuthConfirmPassword(
                      e.target.value
                    )
                  }
                  required
                  style={authInputStyle}
                />
              )}

              {authError && (
                <p
                  style={{
                    color: "var(--auth-error)",
                    marginBottom: "15px",
                  }}
                >
                  {authError}
                </p>
              )}

              <button
                type="submit"
                className="primary-button"
                style={{ width: "100%" }}
                disabled={authLoading}
              >
                {authLoading
                  ? "Please wait..."
                  : authMode === "login"
                  ? "Login"
                  : "Register"}
              </button>
            </form>

            <div
              style={{
                textAlign: "center",
                marginTop: "20px",
              }}
            >
              {authMode === "login" ? (
                <p>
                  Don't have an account?{" "}
                  <button
                    type="button"
                    onClick={() => {
                      setAuthMode("register");
                      setAuthError("");
                    }}
                    style={authLinkStyle}
                  >
                    Register
                  </button>
                </p>
              ) : (
                <p>
                  Already have an account?{" "}
                  <button
                    type="button"
                    onClick={() => {
                      setAuthMode("login");
                      setAuthError("");
                    }}
                    style={authLinkStyle}
                  >
                    Login
                  </button>
                </p>
              )}
            </div>
          </section>
        </main>
      </div>
    );
  }

  // =========================================================
  // COMPLETED SCREEN
  // =========================================================

  if (completed) {
    return (
      <div className="app">
        <header className="navbar">
          <div className="logo">
            AI<span>Interview</span>
          </div>

          <div className="navbar-actions">
            <ThemeToggle />

            <button
              className="profile"
              onClick={resetInterview}
            >
              Dashboard
            </button>
          </div>
        </header>

        <main>
          <section className="section">
            <p className="eyebrow">
              INTERVIEW COMPLETE
            </p>

            <h1>
              Great job! 🎉
            </h1>

            <p className="hero-text">
              You have completed the
              interview.
            </p>

            <div className="feedback-box">
              <h2>Final Score</h2>

              <h1>
                {getFinalScore()} / 100
              </h1>

              <div
                className={`score-badge ${getScoreClass(
                  getFinalScore()
                )}`}
              >
                {getScoreLabel(getFinalScore())}
              </div>

              <p>
                Questions answered:{" "}
                <strong>
                  {scores.length}
                </strong>
              </p>

              <p>
                Performance:{" "}
                <strong>
                  {getPerformanceMessage()}
                </strong>
              </p>
            </div>

            <div className="feedback-box">
              <h2>Question Results</h2>

              {evaluations.length === 0 ? (
                <p>
                  No evaluations available.
                </p>
              ) : (
                evaluations.map(
                  (evaluation, index) => (
                    <div
                      key={index}
                      className="saved-result-card"
                    >
                      <div className="result-label">
                        Question {index + 1}
                      </div>

                      <h3 className="saved-question">
                        {evaluation.question}
                      </h3>

                      <div className="result-divider"></div>

                      <p className="result-label">
                        YOUR ANSWER
                      </p>

                      <p className="result-answer">
                        {evaluation.answer}
                      </p>

                      <div className="result-score-row">
                        <span className="result-label">
                          SCORE
                        </span>

                        <strong>
                          {evaluation.score} / 100
                        </strong>

                        <span
                          className={`score-badge score-badge-small ${getScoreClass(
                            evaluation.score
                          )}`}
                        >
                          {getScoreLabel(evaluation.score)}
                        </span>
                      </div>

                      <div className="result-feedback">
                        <p className="result-label">
                          AI FEEDBACK
                        </p>

                        <p>
                          {evaluation.feedback ||
                            "No feedback available."}
                        </p>
                      </div>
                    </div>
                  )
                )
              )}
            </div>

            <br />

            <button
              className="primary-button"
              onClick={resetInterview}
            >
              Back to Dashboard
            </button>
          </section>
        </main>
      </div>
    );
  }

  // =========================================================
  // SAVED RESULTS SCREEN
  // =========================================================

  if (viewingResults) {
    const interview =
      interviews.find(
        (item) =>
          Number(item.id) ===
          Number(currentInterviewId)
      );

    const finalScore =
      interview?.finalScore ?? 0;

    return (
      <div className="app results-page">
        <header className="navbar">
          <div className="logo">
            AI<span>Interview</span>
          </div>

          <div className="navbar-actions">
            <ThemeToggle />

            <button
              className="profile"
              onClick={resetInterview}
            >
              Back to Dashboard
            </button>
          </div>
        </header>

        <main>
          <section className="section results-section">
            <p className="eyebrow">
              INTERVIEW RESULTS
            </p>

            <h1>
              {interview?.title ||
                "Interview Results"}
            </h1>

            <p className="hero-text">
              Your saved AI interview
              evaluation.
            </p>

            <div className="results-date">
              🕐{" "}
              {formatDateTime(interview)}
            </div>

            <div className="feedback-box result-summary-card">
              <h2>Final Score</h2>

              <h1 className="final-score">
                {finalScore} / 100
              </h1>

              <div
                className={`score-badge ${getScoreClass(
                  finalScore
                )}`}
              >
                {getScoreLabel(finalScore)}
              </div>

              <p className="result-status">
                Status:{" "}
                <strong>
                  {interview?.status ||
                    "COMPLETED"}
                </strong>
              </p>
            </div>

            <div className="saved-results-container">
              <h2 className="results-section-heading">
                Question Results
              </h2>

              {savedResults.length === 0 ? (
                <div className="feedback-box">
                  <p>
                    No saved answers found.
                  </p>
                </div>
              ) : (
                savedResults
                  .slice(0, 5)
                  .map((result, index) => (
                    <div
                      key={
                        result.questionId ||
                        result.id ||
                        index
                      }
                      className="saved-result-card"
                    >
                      <div className="result-label">
                        Question {index + 1}
                      </div>

                      <h3 className="saved-question">
                        {result.question
                          ?.questionText ||
                          result.questionText ||
                          result.question ||
                          "Question"}
                      </h3>

                      <div className="result-divider"></div>

                      <div>
                        <p className="result-label">
                          YOUR ANSWER
                        </p>

                        <p className="result-answer">
                          {result.answerText ||
                            result.answer ||
                            "No answer available."}
                        </p>
                      </div>

                      <div className="result-score-row">
                        <span className="result-label">
                          SCORE
                        </span>

                        <strong>
                          {result.score ?? 0} / 100
                        </strong>

                        <span
                          className={`score-badge score-badge-small ${getScoreClass(
                            result.score ?? 0
                          )}`}
                        >
                          {getScoreLabel(result.score ?? 0)}
                        </span>
                      </div>

                      <div className="result-feedback">
                        <p className="result-label">
                          AI FEEDBACK
                        </p>

                        <p>
                          {result.feedback ||
                            "No feedback available."}
                        </p>
                      </div>
                    </div>
                  ))
              )}
            </div>
          </section>
        </main>
      </div>
    );
  }

  // =========================================================
  // LIVE INTERVIEW SCREEN
  // =========================================================

  if (started) {
    const currentQuestion =
      interviewQuestions[
        currentQuestionIndex
      ];

    if (!currentQuestion) {
      return (
        <div className="app">
          <main>
            <section className="section">
              <h1>
                No question available
              </h1>

              <button
                className="primary-button"
                onClick={resetInterview}
              >
                Back to Dashboard
              </button>
            </section>
          </main>
        </div>
      );
    }

    return (
      <div className="app">
        <header className="navbar">
          <div className="logo">
            AI<span>Interview</span>
          </div>

          <div className="navbar-actions">
            <ThemeToggle />

            <button
              className="profile"
              onClick={() =>
                setStarted(false)
              }
            >
              Back to Dashboard
            </button>
          </div>
        </header>

        <main>
          <section className="section">
            <p className="eyebrow">
              LIVE INTERVIEW
            </p>

            <h1>
              Java Backend Interview
            </h1>

            <p className="hero-text">
              Answer the question below as
              you would in a real interview.
            </p>

            <div className="question-card">
              <div className="question-number">
                {currentQuestionIndex + 1}
              </div>

              <div className="question-content">
                <p>
                  Question{" "}
                  {currentQuestionIndex + 1} of 5
                </p>

                <h2>
                  {currentQuestion.questionText}
                </h2>

                <textarea
                  value={answer}
                  onChange={(event) =>
                    setAnswer(
                      event.target.value
                    )
                  }
                  placeholder="Type your answer here..."
                  rows="8"
                  disabled={submitting}
                />

                <br />

                <button
                  className="primary-button"
                  onClick={submitAnswer}
                  disabled={submitting}
                >
                  {submitting
                    ? "Submitting..."
                    : currentQuestionIndex + 1 >= 5
                    ? "Submit Final Answer"
                    : "Submit Answer →"}
                </button>
              </div>
            </div>
          </section>
        </main>
      </div>
    );
  }

  // =========================================================
  // DASHBOARD DATA
  // =========================================================

  const sortedInterviews = [...interviews].sort(
    (a, b) => {
      const dateB =
        String(
          b.createdAt ??
            b.created_at ??
            b.createdDate ??
            b.created_date ??
            ""
        );

      const dateA =
        String(
          a.createdAt ??
            a.created_at ??
            a.createdDate ??
            a.created_date ??
            ""
        );

      return dateB.localeCompare(dateA);
    }
  );

  const completedInterviews =
    sortedInterviews.filter(
      (interview) =>
        interview.status === "COMPLETED" &&
        interview.finalScore !== null &&
        interview.finalScore !== undefined
    );

  const lastCompletedInterview =
    completedInterviews.length > 0
      ? completedInterviews[0]
      : null;

  // =========================================================
  // DASHBOARD
  // =========================================================

  return (
    <div className="app">
      <header className="navbar">
        <div className="logo">
          AI<span>Interview</span>
        </div>

        <nav>
          <a href="#dashboard">
            Dashboard
          </a>

          <a href="#interviews">
            Interviews
          </a>

          <a href="#questions">
            Questions
          </a>
        </nav>

        <div className="navbar-actions">
          <ThemeToggle />

          <button
            className="profile"
            onClick={handleLogout}
          >
            {localStorage.getItem("userName") ||
              "User"}{" "}
            · Logout
          </button>
        </div>
      </header>

      <main>
        {/* =====================================================
            HERO
        ===================================================== */}

        <section
          className="hero"
          id="dashboard"
        >
          <div>
            <p className="eyebrow">
              AI-POWERED INTERVIEW
              PLATFORM
            </p>

            <h1>
              Practice interviews.
              <br />
              <span>
                Get job ready.
              </span>
            </h1>

            <p className="hero-text">
              Prepare for technical
              interviews with AI-generated
              questions, real-time practice
              and detailed feedback.
            </p>

            <button
              className="primary-button"
              onClick={createNewInterview}
              disabled={loading}
            >
              {loading
                ? "Loading..."
                : "Start Interview"}
            </button>
          </div>

          <div className="hero-card">
            <div className="status">
              <span></span>
              Interview Ready
            </div>

            <h3>
              Java Backend Interview
            </h3>

            <p>
              Software Engineer
            </p>

            <div className="card-line">
              <span>
                Questions
              </span>

              <strong>5</strong>
            </div>

            <div className="card-line">
              <span>
                Status
              </span>

              <strong>
                {lastCompletedInterview
                  ? lastCompletedInterview.status
                  : "—"}
              </strong>
            </div>

            {lastCompletedInterview && (
              <>
                <div className="card-line">
                  <span>
                    Final Score
                  </span>

                  <strong>
                    {
                      lastCompletedInterview.finalScore
                    }{" "}
                    / 100
                  </strong>
                </div>

                <div className="card-line">
                  <span>
                    Date
                  </span>

                  <strong>
                    {formatDateTime(
                      lastCompletedInterview
                    )}
                  </strong>
                </div>
              </>
            )}
          </div>
        </section>

        {/* =====================================================
            ERROR
        ===================================================== */}

        {error && (
          <section className="section">
            <div className="empty">
              <strong>
                Backend connection error
              </strong>

              <p>{error}</p>
            </div>
          </section>
        )}

        {/* =====================================================
            INTERVIEWS
        ===================================================== */}

        <section
          className="section"
          id="interviews"
        >
          <div className="section-heading">
            <div>
              <p className="eyebrow">
                YOUR INTERVIEWS
              </p>

              <h2>
                Interview Dashboard
              </h2>
            </div>
          </div>

          {loading ? (
            <p>
              Loading interviews...
            </p>
          ) : interviews.length === 0 ? (
            <div className="empty">
              No interviews found.
            </div>
          ) : (
            <div className="interview-grid">
              {sortedInterviews.map(
                (interview) => (
                  <div
                    className="interview-card"
                    key={interview.id}
                  >
                    <div className="icon-box">
                      AI
                    </div>

                    <h3>
                      {interview.title}
                    </h3>

                    <p>
                      {interview.role}
                    </p>

                    <div className="interview-date">
                      🕐{" "}
                      {formatDateTime(
                        interview
                      )}
                    </div>

                    <div className="interview-info">
                      <span>
                        Status
                      </span>

                      <strong>
                        {interview.status}
                      </strong>
                    </div>

                    {interview.finalScore !==
                      null &&
                      interview.finalScore !==
                        undefined && (
                        <div className="interview-info">
                          <span>
                            Final Score
                          </span>

                          <strong>
                            {
                              interview.finalScore
                            }{" "}
                            / 100
                          </strong>

                          <span
                            className={`score-badge score-badge-small ${getScoreClass(
                              interview.finalScore
                            )}`}
                          >
                            {getScoreLabel(
                              interview.finalScore
                            )}
                          </span>
                        </div>
                      )}

                    <button
                      className="open-button"
                      onClick={() =>
                        interview.status ===
                        "COMPLETED"
                          ? viewInterviewResults(
                              interview.id
                            )
                          : startInterview(
                              interview.id
                            )
                      }
                    >
                      {interview.status ===
                      "COMPLETED"
                        ? "View Results →"
                        : "Open Interview →"}
                    </button>

                    <button
                      className="delete-button"
                      onClick={() =>
                        deleteInterview(
                          interview.id
                        )
                      }
                    >
                      🗑️ Delete
                    </button>
                  </div>
                )
              )}
            </div>
          )}
        </section>

        {/* =====================================================
            PRACTICE QUESTIONS
        ===================================================== */}

        <section
          className="section"
          id="questions"
        >
          <div className="section-heading">
            <div>
              <p className="eyebrow">
                INTERVIEW QUESTIONS
              </p>

              <h2>
                Practice Questions
              </h2>

              <p className="hero-text">
                10 questions available
                for practice.
              </p>
            </div>
          </div>

          {questions.length === 0 ? (
            <div className="empty">
              No questions available.
            </div>
          ) : (
            <div className="question-list">
              {questions.map(
                (question, index) => (
                  <div
                    className="question-card"
                    key={question.id}
                  >
                    <div className="question-number">
                      {index + 1}
                    </div>

                    <div>
                      <h3>
                        {
                          question.questionText
                        }
                      </h3>

                      <p>
                        Expected answer:{" "}
                        {
                          question.expectedAnswer
                        }
                      </p>
                    </div>
                  </div>
                )
              )}
            </div>
          )}
        </section>
      </main>

      <footer>
        <p>
          © 2026 AI Interview Platform
        </p>

        <p>
          Built with React + Spring Boot
        </p>
      </footer>
    </div>
  );
}

export default App;