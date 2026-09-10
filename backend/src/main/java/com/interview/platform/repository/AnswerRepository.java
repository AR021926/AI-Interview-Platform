
package com.interview.platform.repository;

import com.interview.platform.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionId(Long questionId);

    List<Answer> findByQuestionInterviewId(Long interviewId);

    Optional<Answer> findTopByQuestionIdOrderByIdDesc(Long questionId);
}
