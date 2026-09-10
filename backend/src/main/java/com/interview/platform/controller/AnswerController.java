
package com.interview.platform.controller;

import com.interview.platform.model.Answer;
import com.interview.platform.model.Question;
import com.interview.platform.repository.AnswerRepository;
import com.interview.platform.repository.QuestionRepository;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.service.AIService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/answers")
@CrossOrigin(origins = "http://localhost:5173")
public class AnswerController {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AIService aiService;

    public AnswerController(
            AnswerRepository answerRepository,
            QuestionRepository questionRepository,
            InterviewRepository interviewRepository,
            AIService aiService
    ) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.aiService = aiService;
    }

    // =========================================================
    // SUBMIT ANSWER
    // =========================================================

    @PostMapping
    public ResponseEntity<?> submitAnswer(
            @RequestBody Map<String, Object> request
    ) {

        try {

            Long questionId =
                    Long.valueOf(
                            request.get("questionId").toString()
                    );

            String answerText =
                    request.get("answerText") != null
                            ? request.get("answerText").toString()
                            : "";

            Question question =
                    questionRepository
                            .findById(questionId)
                            .orElse(null);

            if (question == null) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "Question not found"
                                )
                        );
            }

            // =================================================
            // AI EVALUATION
            // =================================================

            AIService.EvaluationResult evaluation =
                        aiService.evaluateAnswer(question, answerText);

            int score =
                    evaluation.getScore();

            String feedback =
                    evaluation.getFeedback();

            // =================================================
            // SAVE ANSWER
            // =================================================

            Answer answer =
                    new Answer();

            answer.setAnswerText(answerText);
            answer.setScore(score);
            answer.setFeedback(feedback);
            answer.setQuestion(question);

            Answer savedAnswer =
                    answerRepository.save(answer);

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "ANSWER SUBMITTED"
            );

            System.out.println(
                    "Question ID: " +
                            questionId
            );

            System.out.println(
                    "Score: " +
                            score
            );

            System.out.println(
                    "Feedback: " +
                            feedback
            );

            System.out.println(
                    "========================================"
            );

            return ResponseEntity.ok(
                    Map.of(
                            "id",
                            savedAnswer.getId(),

                            "questionId",
                            questionId,

                            "answerText",
                            answerText,

                            "score",
                            score,

                            "feedback",
                            feedback
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Failed to submit answer"
                            )
                    );
        }
    }

    // =========================================================
    // GET ANSWERS FOR A QUESTION
    // =========================================================

    @GetMapping("/question/{questionId}")
    public ResponseEntity<?> getAnswersForQuestion(
            @PathVariable Long questionId
    ) {

        try {

            List<Answer> answers =
                    answerRepository
                            .findByQuestionId(questionId);

            return ResponseEntity.ok(answers);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    "Failed to fetch answers"
                            )
                    );
        }
    }
}