package com.interview.platform.config;

import com.interview.platform.model.Question;
import com.interview.platform.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeQuestions(
            QuestionRepository questionRepository) {

        return args -> {

            List<Question> existingQuestions =
                    questionRepository.findByInterviewIsNull();

            if (existingQuestions.size() >= 10) {
                System.out.println(
                        "Practice question pool already contains " +
                        existingQuestions.size() +
                        " questions."
                );
                return;
            }

            List<Question> questions = List.of(

                    new Question(
                            "What is Java?",
                            "Java is a high-level, object-oriented, class-based and platform-independent programming language. Java code is compiled into bytecode that runs on the JVM.",
                            null
                    ),

                    new Question(
                            "What is the difference between JDK, JRE, and JVM?",
                            "JDK is used to develop Java applications and contains development tools. JRE provides the environment required to run Java applications. JVM executes Java bytecode.",
                            null
                    ),

                    new Question(
                            "What are the main features of Java?",
                            "The main features include object-oriented programming, platform independence, portability, security, robustness, multithreading, automatic memory management and high performance through JVM optimizations.",
                            null
                    ),

                    new Question(
                            "What are the four pillars of Object-Oriented Programming?",
                            "The four pillars are encapsulation, inheritance, polymorphism and abstraction.",
                            null
                    ),

                    new Question(
                            "What is the difference between == and equals() in Java?",
                            "The == operator compares primitive values or object references, while equals() is used to compare object content when the class properly overrides the equals method.",
                            null
                    ),

                    new Question(
                            "What is inheritance in Java?",
                            "Inheritance allows one class to acquire properties and behavior from another class using extends. It promotes code reuse and represents an IS-A relationship.",
                            null
                    ),

                    new Question(
                            "What is method overloading and method overriding?",
                            "Method overloading means having multiple methods with the same name but different parameters in the same class. Method overriding means a subclass provides its own implementation of a method inherited from its parent class.",
                            null
                    ),

                    new Question(
                            "What is an exception in Java?",
                            "An exception is an event that disrupts the normal flow of program execution. Java provides try, catch, finally, throw and throws to handle exceptions.",
                            null
                    ),

                    new Question(
                            "What is the difference between ArrayList and LinkedList?",
                            "ArrayList uses a dynamic array and provides fast random access. LinkedList uses linked nodes and is generally more efficient for frequent insertion and deletion in the middle of the list.",
                            null
                    ),

                    new Question(
                            "What is Spring Boot?",
                            "Spring Boot is a framework built on Spring that simplifies the development of Java applications by providing auto-configuration, starter dependencies, embedded servers and production-ready features.",
                            null
                    )
            );

            int startIndex = existingQuestions.size();

            if (startIndex < 10) {
                List<Question> newQuestions =
                        questions.subList(startIndex, 10);

                questionRepository.saveAll(newQuestions);

                System.out.println(
                        "Added " +
                        newQuestions.size() +
                        " global practice questions."
                );
            }
        };
    }
}