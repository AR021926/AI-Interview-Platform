package com.interview.platform.repository;

import com.interview.platform.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByInterviewId(Long interviewId);

    @Query(value = """
            SELECT *
            FROM questions
            WHERE interview_id = :interviewId
            ORDER BY RANDOM()
            LIMIT 10
            """, nativeQuery = true)
    List<Question> findRandomQuestionsByInterviewId(Long interviewId);
}