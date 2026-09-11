# InterviewLab — AI Interview & Coding Assessment Platform

InterviewLab is a full-stack interview preparation platform that simulates technical interviews, evaluates candidate responses using AI, provides structured feedback, and stores interview history and results.

It combines a React frontend, Spring Boot REST API, PostgreSQL database, JWT authentication, and Gemini-powered evaluation with a local fallback mechanism.

---

## Features

### AI-Powered Interview Evaluation
- Conducts technical interview sessions with 5 questions.
- Evaluates candidate answers using Gemini AI when available.
- Produces a score from 0–100 for each answer.
- Generates feedback based on answer quality.
- Uses local fallback evaluation when Gemini is unavailable or quota-limited.
- Includes deterministic validation for blank and copied-question answers.

### Interview Scoring
- Each interview contains exactly 5 questions.
- Every question receives an individual score.
- Final score is calculated across all 5 questions.
- Results include answers, scores, feedback, final score, and performance category.

### Interview Integrity
- Paste operations are blocked in the answer field.
- Candidates are prompted to type answers in their own words.
- Repeating or copying the question as an answer is detected by backend validation.
- Switching away from the interview tab/window terminates the interview.
- Manually exiting an interview also terminates it.
- Unanswered questions in a terminated interview receive a score of 0.

### Authentication & Security
- User registration and login.
- JWT-based authentication.
- Protected backend endpoints.
- Interview ownership validation.
- Environment-based secrets and configuration.
- Configurable CORS origins.

### Dashboard
- Interview history.
- Interview status.
- Final scores.
- Performance indicators.
- Detailed result view.
- Practice questions.
- Completed and terminated interview records.

### User Interface
- Responsive React interface.
- Dashboard and interview views.
- Light/dark appearance support.
- Clear question and answer hierarchy.
- Score and feedback presentation.
- Interview status badges.

---

## Tech Stack

### Frontend
- React
- JavaScript
- Vite
- HTML5
- CSS3
- Fetch API

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- REST APIs
- JWT Authentication
- Maven

### Database
- PostgreSQL

### AI
- Google Gemini API
- Local fallback answer evaluation

---

## System Architecture

```text
                    +----------------------+
                    |      InterviewLab    |
                    |     React / Vite     |
                    +----------+-----------+
                               |
                               | HTTP / REST + JWT
                               v
                    +----------+-----------+
                    |      Spring Boot     |
                    |       REST API       |
                    +----+------------+----+
                         |            |
                         |            |
                         v            v
                +--------+----+   +---+----------------+
                | PostgreSQL  |   | Answer Evaluation  |
                |  Database   |   |      Service       |
                +-------------+   +---------+----------+
                                           |
                                  +--------+---------+
                                  |                  |
                                  v                  v
                           +------+-----+     +------+------+
                           | Gemini API |     | Local       |
                           | Evaluation |     | Fallback    |
                           +------------+     +-------------+
```

---

## Interview Flow

```text
User Login
    |
    v
Dashboard
    |
    v
Start Interview
    |
    v
Create Interview
    |
    v
Load 5 Questions
    |
    v
Candidate Types Answer
    |
    v
Backend Validation
    |
    +---- Blank / copied question ----> Score 0
    |
    v
AI Evaluation
    |
    +---- Gemini Available ----> Gemini Score + Feedback
    |
    +---- Gemini Unavailable --> Local Fallback
    |
    v
Save Answer
    |
    v
Next Question
    |
    v
Complete 5 Questions
    |
    v
Calculate Final Score
    |
    v
Save Interview Result
    |
    v
Results / Dashboard
```

---

## Interview Termination Flow

```text
Interview In Progress
        |
        +---- Exit Interview
        |
        +---- Switch Tab / Window
        |
        v
POST /api/interviews/{id}/terminate
        |
        v
Validate Interview Ownership
        |
        v
Preserve Submitted Scores
        |
        v
Create Missing Answers with Score 0
        |
        v
Calculate Final Score Across 5 Questions
        |
        v
Status = TERMINATED
        |
        v
Save to PostgreSQL
        |
        v
Display Result in Dashboard
```

---

## AI Evaluation Flow

```text
Candidate Answer
       |
       v
Deterministic Validation
       |
       +---- Invalid / copied / blank ---> 0
       |
       v
Gemini Evaluation
       |
       +---- Success ---> AI Score + Feedback
       |
       +---- Error / Limit
                    |
                    v
             Local Evaluation
                    |
                    v
             Score + Feedback
       |
       v
Save Evaluation
```

The fallback mechanism allows the application to continue evaluating interviews when the external AI service is temporarily unavailable.

---

## Project Structure

```text
AI-interview-platform/
|
+-- backend/
|   +-- src/main/java/com/interview/platform/
|   |   +-- controller/
|   |   +-- service/
|   |   +-- repository/
|   |   +-- model/
|   |   +-- security/
|   |   +-- config/
|   |
|   +-- src/main/resources/
|   +-- pom.xml
|
+-- frontend/
|   +-- src/
|   |   +-- App.jsx
|   |   +-- App.css
|   |
|   +-- index.html
|   +-- package.json
|
+-- README.md
```

---

## Environment Configuration

Do not store passwords, JWT secrets, or API keys directly in source control.

### Backend

```env
DB_URL=jdbc:postgresql://localhost:5432/interview_platform
DB_USERNAME=interview_user
DB_PASSWORD=your_database_password

JWT_SECRET=your_secure_jwt_secret
GEMINI_API_KEY=your_gemini_api_key

CORS_ALLOWED_ORIGINS=http://localhost:5173
PORT=8080
```

`JWT_SECRET` should contain at least 32 characters.

### Frontend

```env
VITE_API_URL=http://localhost:8080
```

---

## Running Locally

### Prerequisites
- Java 21
- Node.js and npm
- PostgreSQL
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/AR021926/AI-Interview-Platform.git
cd AI-Interview-Platform
```

### 2. Start PostgreSQL

Create/configure the PostgreSQL database and provide backend database credentials through environment variables.

### 3. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### 4. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Production Build

### Frontend

```bash
cd frontend
npm run build
```

### Backend

```powershell
cd backend
.\mvnw.cmd clean compile -DskipTests
```

---

## Security Notes

- Secrets are supplied through environment variables.
- JWT protects authenticated API requests.
- Backend endpoints validate interview ownership.
- API keys and database passwords should never be committed.
- CORS origins can be configured for local and deployed environments.

---

## Key Engineering Challenges

This project goes beyond a basic CRUD application by handling:

- AI-service availability and quota limitations.
- Deterministic validation before AI evaluation.
- Maintaining consistent interview scoring.
- Persisting interview state and answer history.
- Authentication and authorization.
- Handling incomplete and terminated interviews.
- Tab-switch detection.
- Preventing pasted interview responses.
- Preserving submitted scores when an interview is terminated.
- Keeping frontend, backend, database, and AI evaluation synchronized.

---

## Current Status

The core application flow has been implemented and locally tested:

- Registration and login
- JWT authentication
- Interview creation
- Five-question interview flow
- AI answer evaluation
- Local evaluation fallback
- Individual question scoring
- Final score calculation
- PostgreSQL persistence
- Interview history
- Detailed results
- Paste prevention
- Copied-question validation
- Manual interview termination
- Tab-switch interview termination
- Terminated interview result handling
- Production frontend and backend build verification

Deployment is the next project milestone.

---

## Future Improvements

- Cloud deployment
- Coding assessment execution
- Additional interview categories
- Difficulty selection
- More detailed performance analytics
- Improved AI scoring calibration
- Automated backend/frontend tests

---

## Author

**Angoth Ramesh**

GitHub: [AR021926](https://github.com/AR021926)

---

## Disclaimer

InterviewLab is an interview preparation and assessment project. AI-generated scores and feedback are intended to assist practice and should not be treated as definitive measures of a candidate's technical ability.
