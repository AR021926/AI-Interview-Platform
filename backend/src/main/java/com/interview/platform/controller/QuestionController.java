package com.interview.platform.controller;

import com.interview.platform.model.Question;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.repository.QuestionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;

    public QuestionController(
            QuestionRepository questionRepository,
            InterviewRepository interviewRepository) {

        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
    }

    // ==========================================
    // CREATE QUESTION
    // ==========================================

    @PostMapping
    public ResponseEntity<?> createQuestion(
            @RequestBody QuestionRequest request) {

        return interviewRepository.findById(request.interviewId())
                .map(interview -> {

                    Question question = new Question(
                            request.questionText(),
                            request.expectedAnswer(),
                            interview
                    );

                    return ResponseEntity.ok(
                            questionRepository.save(question)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // GET 10 PRACTICE QUESTIONS
    // ==========================================

    @GetMapping
    public List<Question> getPracticeQuestions() {

        return questionRepository.findAll()
                .stream()
                .limit(10)
                .toList();
    }

    // ==========================================
    // GET ALL QUESTIONS FOR AN INTERVIEW
    // ==========================================

    @GetMapping("/interview/{interviewId}")
    public List<Question> getQuestionsByInterview(
            @PathVariable Long interviewId) {

        return questionRepository.findByInterviewId(interviewId);
    }

    // ==========================================
    // GET 5 RANDOM INTERVIEW QUESTIONS
    // ==========================================

    @GetMapping("/interview/{interviewId}/random")
    public ResponseEntity<?> getRandomQuestions(
            @PathVariable Long interviewId) {

        if (!interviewRepository.existsById(interviewId)) {
            return ResponseEntity.notFound().build();
        }

        List<Question> questions =
                questionRepository.findRandomQuestionsByInterviewId(
                        interviewId
                );

        // IMPORTANT:
        // Live Interview must contain ONLY 5 questions.
        if (questions.size() > 5) {
            questions = questions.subList(0, 5);
        }

        System.out.println(
                "========================================"
        );

        System.out.println(
                "LIVE INTERVIEW QUESTIONS"
        );

        System.out.println(
                "Interview ID: " + interviewId
        );

        System.out.println(
                "Questions returned: " + questions.size()
        );

        System.out.println(
                "========================================"
        );

        return ResponseEntity.ok(questions);
    }

    // ==========================================
    // REQUEST BODY
    // ==========================================

    public record QuestionRequest(
            String questionText,
            String expectedAnswer,
            Long interviewId
    ) {
    }
}