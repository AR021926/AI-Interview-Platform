package com.interview.platform.controller;

import com.interview.platform.model.Answer;
import com.interview.platform.model.Interview;
import com.interview.platform.model.Question;
import com.interview.platform.model.User;
import com.interview.platform.repository.AnswerRepository;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.repository.QuestionRepository;
import com.interview.platform.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public InterviewController(
            InterviewRepository interviewRepository,
            UserRepository userRepository,
            AnswerRepository answerRepository,
            QuestionRepository questionRepository
    ) {
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    // =========================================================
    // CREATE NEW INTERVIEW
    // =========================================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> createInterview(
            @RequestBody InterviewRequest request,
            Principal principal
    ) {

        User authenticatedUser = getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        Interview interview = new Interview(
                request.title(),
                request.role(),
                request.status(),
                authenticatedUser
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        /*
         * Preserve the existing project behaviour:
         * copy up to 10 existing questions into the
         * newly created interview.
         */
        List<Question> existingQuestions =
                questionRepository.findAll();

        existingQuestions.stream()
                .limit(10)
                .forEach(oldQuestion -> {

                    Question newQuestion = new Question(
                            oldQuestion.getQuestionText(),
                            oldQuestion.getExpectedAnswer(),
                            savedInterview
                    );

                    questionRepository.save(newQuestion);
                });

        return ResponseEntity.ok(savedInterview);
    }

    // =========================================================
    // GET AUTHENTICATED USER'S INTERVIEWS
    // =========================================================
    @GetMapping
    public ResponseEntity<?> getInterviews(
            Principal principal
    ) {

        User authenticatedUser = getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        List<Interview> userInterviews =
                interviewRepository.findAll()
                        .stream()
                        .filter(interview ->
                                belongsToUser(
                                        interview,
                                        authenticatedUser
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        Interview::getId
                                ).reversed()
                        )
                        .toList();

        return ResponseEntity.ok(userInterviews);
    }

    // =========================================================
    // GET INTERVIEW RESULTS
    // =========================================================
    @GetMapping("/{interviewId}/results")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInterviewResults(
            @PathVariable Long interviewId,
            Principal principal
    ) {

        User authenticatedUser =
                getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        Optional<Interview> optionalInterview =
                interviewRepository.findById(interviewId);

        if (optionalInterview.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Interview interview =
                optionalInterview.get();

        if (!belongsToUser(
                interview,
                authenticatedUser
        )) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You do not have permission to access this interview."
                    );
        }

        List<Question> questions =
                questionRepository.findByInterviewId(
                        interviewId
                );

        List<Answer> allAnswers =
                answerRepository.findByQuestionInterviewId(
                        interviewId
                );

        Map<Long, Answer> latestAnswerByQuestion =
                allAnswers.stream()
                        .filter(answer ->
                                answer != null
                        )
                        .filter(answer ->
                                answer.getQuestion() != null
                        )
                        .filter(answer ->
                                answer.getQuestion().getId() != null
                        )
                        .filter(answer ->
                                answer.getId() != null
                        )
                        .collect(
                                Collectors.toMap(
                                        answer ->
                                                answer.getQuestion().getId(),

                                        Function.identity(),

                                        (answer1, answer2) ->
                                                answer1.getId()
                                                        > answer2.getId()
                                                        ? answer1
                                                        : answer2
                                )
                        );

        /*
         * Answer ID order represents the order
         * in which answers were submitted.
         */
        List<Question> answeredQuestions =
                questions.stream()
                        .filter(question ->
                                latestAnswerByQuestion.containsKey(
                                        question.getId()
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        question ->
                                                latestAnswerByQuestion
                                                        .get(
                                                                question.getId()
                                                        )
                                                        .getId()
                                )
                        )
                        .limit(5)
                        .toList();

        List<Map<String, Object>> results =
                answeredQuestions.stream()
                        .map(question -> {

                            Answer answer =
                                    latestAnswerByQuestion.get(
                                            question.getId()
                                    );

                            Map<String, Object> result =
                                    new java.util.HashMap<>();

                            result.put(
                                    "questionId",
                                    question.getId()
                            );

                            result.put(
                                    "question",
                                    question.getQuestionText()
                            );

                            result.put(
                                    "answer",
                                    answer.getAnswerText() == null
                                            ? ""
                                            : answer.getAnswerText()
                            );

                            result.put(
                                    "answerText",
                                    answer.getAnswerText() == null
                                            ? ""
                                            : answer.getAnswerText()
                            );

                            result.put(
                                    "score",
                                    answer.getScore() == null
                                            ? 0
                                            : answer.getScore()
                            );

                            result.put(
                                    "feedback",
                                    answer.getFeedback() == null
                                            ? "No feedback available."
                                            : answer.getFeedback()
                            );

                            return result;
                        })
                        .toList();

        return ResponseEntity.ok(results);
    }

    // =========================================================
    // DELETE INTERVIEW
    // =========================================================
    @DeleteMapping("/{interviewId}")
    @Transactional
    public ResponseEntity<?> deleteInterview(
            @PathVariable Long interviewId,
            Principal principal
    ) {

        User authenticatedUser =
                getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        Optional<Interview> optionalInterview =
                interviewRepository.findById(interviewId);

        if (optionalInterview.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Interview interview =
                optionalInterview.get();

        if (!belongsToUser(
                interview,
                authenticatedUser
        )) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You do not have permission to delete this interview."
                    );
        }

        List<Answer> answers =
                answerRepository.findByQuestionInterviewId(
                        interviewId
                );

        if (!answers.isEmpty()) {
            answerRepository.deleteAll(answers);
        }

        List<Question> questions =
                questionRepository.findByInterviewId(
                        interviewId
                );

        if (!questions.isEmpty()) {
            questionRepository.deleteAll(questions);
        }

        interviewRepository.delete(interview);

        return ResponseEntity.ok(
                "Interview deleted successfully."
        );
    }

    // =========================================================
    // COMPLETE INTERVIEW
    // =========================================================
    @PostMapping("/{interviewId}/complete")
    @Transactional
    public ResponseEntity<?> completeInterview(
            @PathVariable Long interviewId,
            @RequestBody(required = false)
            CompleteInterviewRequest request,
            Principal principal
    ) {

        User authenticatedUser =
                getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        Optional<Interview> optionalInterview =
                interviewRepository.findById(interviewId);

        if (optionalInterview.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Interview interview =
                optionalInterview.get();

        if (!belongsToUser(
                interview,
                authenticatedUser
        )) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You do not have permission to complete this interview."
                    );
        }

        List<Answer> allAnswers =
                answerRepository.findByQuestionInterviewId(
                        interviewId
                );

        if (allAnswers.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            "No answers found for this interview."
                    );
        }

        /*
         * The frontend sends the EXACT 5 question IDs
         * used during the live interview.
         */
        List<Long> questionIds;

        if (request != null
                && request.questionIds() != null
                && !request.questionIds().isEmpty()) {

            questionIds =
                    request.questionIds();

        } else {

            /*
             * Fallback:
             * Find the newest submitted answer for
             * every question and use the latest 5.
             */
            Map<Long, Answer> latestAnswerByQuestion =
                    allAnswers.stream()
                            .filter(answer ->
                                    answer != null
                            )
                            .filter(answer ->
                                    answer.getQuestion() != null
                            )
                            .filter(answer ->
                                    answer.getQuestion().getId() != null
                            )
                            .filter(answer ->
                                    answer.getId() != null
                            )
                            .collect(
                                    Collectors.toMap(
                                            answer ->
                                                    answer
                                                            .getQuestion()
                                                            .getId(),

                                            Function.identity(),

                                            (answer1, answer2) ->
                                                    answer1.getId()
                                                            > answer2.getId()
                                                            ? answer1
                                                            : answer2
                                    )
                            );

            questionIds =
                    latestAnswerByQuestion.values()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            Answer::getId
                                    ).reversed()
                            )
                            .limit(5)
                            .map(answer ->
                                    answer
                                            .getQuestion()
                                            .getId()
                            )
                            .toList();
        }

        /*
         * A live interview MUST contain
         * exactly 5 answered questions.
         */
        if (questionIds.size() < 5) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            "Interview requires 5 answered questions. Only "
                                    + questionIds.size()
                                    + " were found."
                    );
        }

        /*
         * Always process exactly 5 questions.
         */
        questionIds =
                questionIds.stream()
                        .limit(5)
                        .toList();

        final List<Long> selectedQuestionIds =
                questionIds;

        /*
         * Get answers belonging ONLY
         * to these exact 5 questions.
         */
        List<Answer> attemptAnswers =
                allAnswers.stream()
                        .filter(answer ->
                                answer != null
                        )
                        .filter(answer ->
                                answer.getQuestion() != null
                        )
                        .filter(answer ->
                                answer.getQuestion().getId() != null
                        )
                        .filter(answer ->
                                answer.getId() != null
                        )
                        .filter(answer ->
                                selectedQuestionIds.contains(
                                        answer
                                                .getQuestion()
                                                .getId()
                                )
                        )
                        .toList();

        /*
         * If a question was submitted more than once,
         * use ONLY the newest answer.
         */
        Map<Long, Answer> latestAnswersByQuestion =
                attemptAnswers.stream()
                        .collect(
                                Collectors.toMap(
                                        answer ->
                                                answer
                                                        .getQuestion()
                                                        .getId(),

                                        Function.identity(),

                                        (answer1, answer2) ->
                                                answer1.getId()
                                                        > answer2.getId()
                                                        ? answer1
                                                        : answer2
                                )
                        );

        /*
         * Get one newest answer for
         * each of the exact 5 questions.
         */
        List<Answer> latestAnswers =
                selectedQuestionIds.stream()
                        .map(
                                latestAnswersByQuestion::get
                        )
                        .filter(answer ->
                                answer != null
                        )
                        .toList();

        if (latestAnswers.size() < 5) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(
                            "Some answers are still being evaluated. "
                                    + "Please wait a few seconds and try again."
                    );
        }

        /*
         * Check AI/fallback evaluation is finished.
         */
        List<Answer> processingAnswers =
                latestAnswers.stream()
                        .filter(this::isProcessing)
                        .toList();

        if (!processingAnswers.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(
                            "AI evaluation is still processing. "
                                    + "Please wait a few seconds and try again."
                    );
        }

        /*
         * All five questions must have valid scores.
         */
        List<Answer> scoredAnswers =
                latestAnswers.stream()
                        .filter(answer ->
                                answer.getScore() != null
                        )
                        .filter(answer ->
                                !isProcessing(answer)
                        )
                        .toList();

        if (scoredAnswers.size() < 5) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(
                            "Some answers are still being evaluated. "
                                    + "Please wait a few seconds and try again."
                    );
        }

        /*
         * FINAL SCORE:
         *
         * Average of EXACTLY these 5 question scores
         * from THIS interview attempt.
         *
         * Different interview attempts are NOT
         * averaged together.
         */
        int totalScore =
                scoredAnswers.stream()
                        .mapToInt(Answer::getScore)
                        .sum();

        int finalScore =
                Math.round(
                        (float) totalScore / 5
                );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "COMPLETING INTERVIEW"
        );

        System.out.println(
                "Interview ID: " + interviewId
        );

        System.out.println(
                "Question IDs: " + selectedQuestionIds
        );

        System.out.println(
                "Number of answers: "
                        + scoredAnswers.size()
        );

        System.out.println(
                "Total score: " + totalScore
        );

        System.out.println(
                "Final score: " + finalScore
        );

        for (Answer answer : scoredAnswers) {

            System.out.println(
                    "Question ID: "
                            + answer
                            .getQuestion()
                            .getId()
                            + " | Score: "
                            + answer.getScore()
            );
        }

        System.out.println(
                "========================================"
        );

        interview.setFinalScore(
                finalScore
        );

        interview.setStatus(
                "COMPLETED"
        );

        Interview savedInterview =
                interviewRepository.save(
                        interview
                );

        return ResponseEntity.ok(
                savedInterview
        );
    }

    // =========================================================
    // AUTHENTICATED USER FROM JWT
    // =========================================================
    // =========================================================
    // TERMINATE INTERVIEW
    // =========================================================
    @PostMapping("/{interviewId}/terminate")
    @Transactional
    public ResponseEntity<?> terminateInterview(
            @PathVariable Long interviewId,
            @RequestBody CompleteInterviewRequest request,
            Principal principal
    ) {

        User authenticatedUser =
                getAuthenticatedUser(principal);

        if (authenticatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required.");
        }

        Optional<Interview> optionalInterview =
                interviewRepository.findById(interviewId);

        if (optionalInterview.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Interview interview =
                optionalInterview.get();

        if (!belongsToUser(
                interview,
                authenticatedUser
        )) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You do not have permission to terminate this interview."
                    );
        }

        if ("COMPLETED".equalsIgnoreCase(
                interview.getStatus()
        )) {
            return ResponseEntity.ok(interview);
        }

        if (request == null
                || request.questionIds() == null
                || request.questionIds().size() != 5) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Exactly 5 question IDs are required."
                    );
        }

        List<Long> questionIds =
                request.questionIds()
                        .stream()
                        .distinct()
                        .limit(5)
                        .toList();

        if (questionIds.size() != 5) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            "Exactly 5 unique question IDs are required."
                    );
        }

        List<Answer> allAnswers =
                answerRepository
                        .findByQuestionInterviewId(
                                interviewId
                        );

        Map<Long, Answer> latestAnswers =
                allAnswers.stream()
                        .filter(answer ->
                                answer != null
                        )
                        .filter(answer ->
                                answer.getQuestion() != null
                        )
                        .filter(answer ->
                                answer.getQuestion().getId() != null
                        )
                        .filter(answer ->
                                answer.getId() != null
                        )
                        .collect(
                                Collectors.toMap(
                                        answer ->
                                                answer
                                                        .getQuestion()
                                                        .getId(),

                                        Function.identity(),

                                        (first, second) ->
                                                first.getId()
                                                        > second.getId()
                                                        ? first
                                                        : second
                                )
                        );

        int totalScore = 0;

        for (Long questionId : questionIds) {

            Question question =
                    questionRepository
                            .findById(questionId)
                            .orElse(null);

            if (question == null
                    || question.getInterview() == null
                    || question.getInterview().getId() == null
                    || !question
                            .getInterview()
                            .getId()
                            .equals(interviewId)) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid interview question."
                        );
            }

            Answer existing =
                    latestAnswers.get(questionId);

            if (existing != null) {

                Integer score =
                        existing.getScore();

                if (score != null &&
                        !isProcessing(existing)) {

                    totalScore +=
                            Math.max(
                                    0,
                                    Math.min(
                                            100,
                                            score
                                    )
                            );
                }

                continue;
            }

            Answer unanswered =
                    new Answer(
                            "Not answered",
                            0,
                            "This question was not answered because the interview was terminated.",
                            question
                    );

            answerRepository.save(
                    unanswered
            );
        }

        int finalScore =
                Math.round(
                        (float) totalScore / 5
                );

        interview.setFinalScore(
                finalScore
        );

        interview.setStatus(
                "TERMINATED"
        );

        Interview savedInterview =
                interviewRepository.save(
                        interview
                );

        return ResponseEntity.ok(
                savedInterview
        );
    }

    private User getAuthenticatedUser(
            Principal principal
    ) {

        if (principal == null
                || principal.getName() == null
                || principal.getName().isBlank()) {

            return null;
        }

        return userRepository
                .findByEmail(
                        principal.getName()
                )
                .orElse(null);
    }

    // =========================================================
    // OWNERSHIP CHECK
    // =========================================================
    private boolean belongsToUser(
            Interview interview,
            User user
    ) {

        if (interview == null
                || user == null
                || user.getId() == null
                || interview.getUser() == null
                || interview.getUser().getId() == null) {

            return false;
        }

        return user.getId().equals(
                interview
                        .getUser()
                        .getId()
        );
    }

    // =========================================================
    // EVALUATION PROCESSING CHECK
    // =========================================================
    private boolean isProcessing(
            Answer answer
    ) {

        if (answer == null) {
            return true;
        }

        if (answer.getScore() == null) {
            return true;
        }

        String feedback =
                answer.getFeedback();

        if (feedback == null
                || feedback.isBlank()) {

            return true;
        }

        return feedback.trim().equals(
                "Answer submitted. AI evaluation is processing."
        );
    }

    /*
     * userId stays temporarily because the existing
     * frontend may still send it.
     *
     * SECURITY:
     * The backend completely ignores this userId.
     * Ownership comes from the JWT authenticated user.
     */
    public record InterviewRequest(
            String title,
            String role,
            String status,
            Long userId
    ) {
    }

    public record CompleteInterviewRequest(
            List<Long> questionIds
    ) {
    }
}