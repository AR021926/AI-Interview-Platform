package com.interview.platform.controller;

import com.interview.platform.model.Answer;
import com.interview.platform.model.Interview;
import com.interview.platform.model.Question;
import com.interview.platform.repository.AnswerRepository;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.repository.QuestionRepository;
import com.interview.platform.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
            QuestionRepository questionRepository) {

        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    // CREATE NEW INTERVIEW
    @PostMapping
    @Transactional
    public ResponseEntity<?> createInterview(
            @RequestBody InterviewRequest request) {

        if (request.userId() == null) {
            return ResponseEntity.badRequest()
                    .body("User ID cannot be null.");
        }

        return userRepository.findById(request.userId())
                .map(user -> {

                    Interview interview = new Interview(
                            request.title(),
                            request.role(),
                            request.status(),
                            user
                    );

                    Interview savedInterview =
                            interviewRepository.save(interview);

                    // Every interview has 10 questions.
                    List<Question> existingQuestions =
                            questionRepository.findAll();

                    existingQuestions.stream()
                            .limit(10)
                            .forEach(oldQuestion -> {

                                Question newQuestion =
                                        new Question(
                                                oldQuestion.getQuestionText(),
                                                oldQuestion.getExpectedAnswer(),
                                                savedInterview
                                        );

                                questionRepository.save(newQuestion);
                            });

                    return ResponseEntity.ok(savedInterview);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET ONLY THE LOGGED-IN USER'S INTERVIEWS
    @GetMapping
    public ResponseEntity<?> getInterviews(
            @RequestParam Long userId) {

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body("User ID is required.");
        }

        return userRepository.findById(userId)
                .map(user -> {

                    List<Interview> userInterviews =
                            interviewRepository.findAll()
                                    .stream()
                                    .filter(interview ->
                                            interview.getUser() != null
                                                    && interview.getUser().getId() != null
                                                    && userId.equals(
                                                            interview.getUser().getId()
                                                    )
                                    )
                                    .toList();

                    return ResponseEntity.ok(userInterviews);
                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    // GET INTERVIEW RESULTS
    @GetMapping("/{interviewId}/results")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInterviewResults(
            @PathVariable Long interviewId) {

        return interviewRepository.findById(interviewId)
                .map(interview -> {

                    List<Question> questions =
                            questionRepository.findByInterviewId(interviewId);

                    List<Answer> allAnswers =
                            answerRepository.findByQuestionInterviewId(
                                    interviewId
                            );

                    Map<Long, Answer> latestAnswerByQuestion =
                            allAnswers.stream()
                                    .filter(answer -> answer != null)
                                    .filter(answer -> answer.getQuestion() != null)
                                    .filter(answer -> answer.getQuestion().getId() != null)
                                    .filter(answer -> answer.getId() != null)
                                    .collect(
                                            Collectors.toMap(
                                                    answer ->
                                                            answer.getQuestion().getId(),
                                                    Function.identity(),
                                                    (answer1, answer2) ->
                                                            answer1.getId() > answer2.getId()
                                                                    ? answer1
                                                                    : answer2
                                            )
                                    );

                    /*
                     * IMPORTANT:
                     * Sort questions according to Answer ID.
                     *
                     * Answer ID order represents the order in which
                     * answers were submitted.
                     */
                    List<Question> answeredQuestions =
                            questions.stream()
                                    .filter(question ->
                                            latestAnswerByQuestion.containsKey(
                                                    question.getId()
                                            ))
                                    .sorted(
                                            Comparator.comparing(
                                                    question ->
                                                            latestAnswerByQuestion
                                                                    .get(question.getId())
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

                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE INTERVIEW
    @DeleteMapping("/{interviewId}")
    @Transactional
    public ResponseEntity<?> deleteInterview(
            @PathVariable Long interviewId) {

        return interviewRepository.findById(interviewId)
                .map(interview -> {

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
                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    // COMPLETE INTERVIEW
    @PostMapping("/{interviewId}/complete")
    @Transactional
    public ResponseEntity<?> completeInterview(
            @PathVariable Long interviewId,
            @RequestBody(required = false)
            CompleteInterviewRequest request) {

        return interviewRepository.findById(interviewId)
                .map(interview -> {

                    List<Answer> allAnswers =
                            answerRepository.findByQuestionInterviewId(
                                    interviewId
                            );

                    if (allAnswers.isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body("No answers found for this interview.");
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
                         * Find the latest answer for every question,
                         * then use the newest 5 questions.
                         */
                        Map<Long, Answer> latestAnswerByQuestion =
                                allAnswers.stream()
                                        .filter(answer -> answer != null)
                                        .filter(answer -> answer.getQuestion() != null)
                                        .filter(answer ->
                                                answer.getQuestion().getId() != null)
                                        .filter(answer ->
                                                answer.getId() != null)
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
                                                answer.getQuestion().getId()
                                        )
                                        .toList();
                    }

                    /*
                     * A live interview MUST contain exactly 5 questions.
                     */
                    if (questionIds.size() < 5) {

                        return ResponseEntity.status(400)
                                .body(
                                        "Interview requires 5 answered questions. Only "
                                                + questionIds.size()
                                                + " were found."
                                );
                    }

                    /*
                     * Always process exactly 5.
                     */
                    questionIds =
                            questionIds.stream()
                                    .limit(5)
                                    .toList();

                    final List<Long> selectedQuestionIds =
                            questionIds;

                    /*
                     * Get answers belonging ONLY to these 5 questions.
                     */
                    List<Answer> attemptAnswers =
                            allAnswers.stream()
                                    .filter(answer -> answer != null)
                                    .filter(answer -> answer.getQuestion() != null)
                                    .filter(answer ->
                                            answer.getQuestion().getId() != null)
                                    .filter(answer ->
                                            answer.getId() != null)
                                    .filter(answer ->
                                            selectedQuestionIds.contains(
                                                    answer.getQuestion().getId()
                                            ))
                                    .toList();

                    /*
                     * If a question has multiple submitted answers,
                     * use ONLY the newest answer.
                     */
                    Map<Long, Answer> latestAnswersByQuestion =
                            attemptAnswers.stream()
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
                     * Get exactly one latest answer per selected question.
                     */
                    List<Answer> latestAnswers =
                            selectedQuestionIds.stream()
                                    .map(latestAnswersByQuestion::get)
                                    .filter(answer -> answer != null)
                                    .toList();

                    /*
                     * We need exactly 5 answers.
                     */
                    if (latestAnswers.size() < 5) {

                        return ResponseEntity.status(202)
                                .body(
                                        "Some answers are still being evaluated. "
                                                + "Please wait a few seconds and try again."
                                );
                    }

                    /*
                     * Check that AI evaluation has finished.
                     */
                    List<Answer> processingAnswers =
                            latestAnswers.stream()
                                    .filter(this::isProcessing)
                                    .toList();

                    if (!processingAnswers.isEmpty()) {

                        return ResponseEntity.status(202)
                                .body(
                                        "AI evaluation is still processing. "
                                                + "Please wait a few seconds and try again."
                                );
                    }

                    /*
                     * Make sure all 5 have valid scores.
                     */
                    List<Answer> scoredAnswers =
                            latestAnswers.stream()
                                    .filter(answer ->
                                            answer.getScore() != null)
                                    .filter(answer ->
                                            !isProcessing(answer))
                                    .toList();

                    if (scoredAnswers.size() < 5) {

                        return ResponseEntity.status(202)
                                .body(
                                        "Some answers are still being evaluated. "
                                                + "Please wait a few seconds and try again."
                                );
                    }

                    /*
                     * Calculate the final score from EXACTLY these 5 answers.
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
                            "Number of answers: " + scoredAnswers.size()
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
                                        + answer.getQuestion().getId()
                                        + " | Score: "
                                        + answer.getScore()
                        );
                    }

                    System.out.println(
                            "========================================"
                    );

                    /*
                     * SAVE FINAL SCORE TO DATABASE.
                     */
                    interview.setFinalScore(finalScore);
                    interview.setStatus("COMPLETED");

                    Interview savedInterview =
                            interviewRepository.save(interview);

                    /*
                     * Return the saved interview.
                     */
                    return ResponseEntity.ok(savedInterview);

                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    private boolean isProcessing(Answer answer) {

        if (answer == null) {
            return true;
        }

        if (answer.getScore() == null) {
            return true;
        }

        String feedback =
                answer.getFeedback();

        if (feedback == null || feedback.isBlank()) {
            return true;
        }

        return feedback.trim().equals(
                "Answer submitted. AI evaluation is processing."
        );
    }

    public record InterviewRequest(
            String title,
            String role,
            String status,
            Long userId
    ) {}

    public record CompleteInterviewRequest(
            List<Long> questionIds
    ) {}
}