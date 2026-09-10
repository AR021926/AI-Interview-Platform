package com.interview.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public Answer() {
    }

    public Answer(
            String answerText,
            Integer score,
            String feedback,
            Question question
    ) {
        this.answerText = answerText;
        this.score = score;
        this.feedback = feedback;
        this.question = question;
    }

    public Long getId() {
        return id;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}