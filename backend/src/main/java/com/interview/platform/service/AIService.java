package com.interview.platform.service;

import com.interview.platform.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EvaluationResult evaluateAnswer(
            Question question,
            String answerText
    ) {

        String questionText =
                question != null && question.getQuestionText() != null
                        ? question.getQuestionText()
                        : "";

        String candidateAnswer =
                answerText != null
                        ? answerText
                        : "";

        EvaluationResult validationResult =
                validateCandidateAnswer(
                        questionText,
                        candidateAnswer
                );

        if (validationResult != null) {
            return validationResult;
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Gemini API key not configured. Using fallback.");
            return fallbackEvaluation(question, answerText);
        }

        try {

            String expectedAnswer =
                    question != null && question.getExpectedAnswer() != null
                            ? question.getExpectedAnswer()
                            : "";

            String prompt =
                    "You are an expert technical interviewer.\n\n" +
                    "Evaluate the candidate's answer.\n\n" +
                    "Question:\n" +
                    questionText +
                    "\n\nExpected Answer:\n" +
                    expectedAnswer +
                    "\n\nCandidate Answer:\n" +
                    candidateAnswer +
                    "\n\n" +
                    "Give a score from 0 to 100.\n" +
                    "If the candidate only repeats or closely paraphrases the question instead of answering it, score 0.\n" +
                    "Do not award marks just because words from the question appear in the candidate answer.\n" +
                    "A meaningless or unrelated answer should receive 0 to 5.\n" +
                    "Give short useful feedback.\n\n" +
                    "Return ONLY this format:\n" +
                    "SCORE: number\n" +
                    "FEEDBACK: text";

            String requestBody =
                    "{"
                            + "\"contents\":["
                            + "{"
                            + "\"parts\":["
                            + "{"
                            + "\"text\":\"" + escapeJson(prompt) + "\""
                            + "}"
                            + "]"
                            + "}"
                            + "]"
                            + "}";

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + "gemini-2.0-flash:generateContent?key="
                            + apiKey;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            requestBody
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Gemini HTTP Status: " +
                            response.statusCode()
            );

            System.out.println(
                    "Gemini Response: " +
                            response.body()
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                System.out.println(
                        "Gemini API failed. Using fallback evaluation."
                );

                return fallbackEvaluation(
                        question,
                        answerText
                );
            }

            String generatedText =
                    extractGeneratedText(response.body());

            if (generatedText == null ||
                    generatedText.isBlank()) {

                System.out.println(
                        "Gemini returned no usable text."
                );

                return fallbackEvaluation(
                        question,
                        answerText
                );
            }

            System.out.println(
                    "Gemini Generated Text: " +
                            generatedText
            );

            int score =
                    extractScore(generatedText);

            String feedback =
                    extractFeedback(generatedText);

            return new EvaluationResult(
                    score,
                    feedback
            );

        } catch (Exception e) {

            System.out.println(
                    "Gemini evaluation error: " +
                            e.getMessage()
            );

            e.printStackTrace();

            return fallbackEvaluation(
                    question,
                    answerText
            );
        }
    }

    // =========================================================
    // ANSWER VALIDATION
    // =========================================================

    private EvaluationResult validateCandidateAnswer(
            String questionText,
            String answerText
    ) {

        if (answerText == null ||
                answerText.trim().isEmpty()) {

            return new EvaluationResult(
                    0,
                    "No answer was provided."
            );
        }

        String normalizedQuestion =
                normalizeText(questionText);

        String normalizedAnswer =
                normalizeText(answerText);

        if (!normalizedQuestion.isBlank() &&
                normalizedAnswer.equals(normalizedQuestion)) {

            return new EvaluationResult(
                    0,
                    "The response only repeats the interview question. Explain the concept in your own words and include the key technical points."
            );
        }

        if (!normalizedQuestion.isBlank() &&
                isMostlyQuestionRepetition(
                        normalizedQuestion,
                        normalizedAnswer
                )) {

            return new EvaluationResult(
                    0,
                    "The response mostly repeats the interview question instead of answering it. Provide a direct technical explanation."
            );
        }

        return null;
    }

    private boolean isMostlyQuestionRepetition(
            String normalizedQuestion,
            String normalizedAnswer
    ) {

        String[] questionWords =
                normalizedQuestion.split("\\s+");

        String[] answerWords =
                normalizedAnswer.split("\\s+");

        if (questionWords.length < 3 ||
                answerWords.length < 3) {
            return false;
        }

        int meaningfulAnswerWords = 0;
        int matchedWords = 0;

        for (String answerWord : answerWords) {

            if (answerWord.length() < 3) {
                continue;
            }

            meaningfulAnswerWords++;

            for (String questionWord : questionWords) {

                if (answerWord.equals(questionWord)) {
                    matchedWords++;
                    break;
                }
            }
        }

        if (meaningfulAnswerWords == 0) {
            return false;
        }

        double overlap =
                (double) matchedWords /
                        meaningfulAnswerWords;

        return overlap >= 0.85 &&
                answerWords.length <=
                        questionWords.length + 3;
    }

    private String normalizeText(String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9\\s]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // =========================================================
    // FALLBACK EVALUATION
    // =========================================================

    private EvaluationResult fallbackEvaluation(
            Question question,
            String answerText
    ) {

        if (answerText == null ||
                answerText.trim().isEmpty()) {

            return new EvaluationResult(
                    0,
                    "No answer was provided."
            );
        }

        String answer =
                answerText.trim().toLowerCase();

        String expected =
                question != null &&
                        question.getExpectedAnswer() != null
                        ? question.getExpectedAnswer().toLowerCase()
                        : "";

        int score = 50;

        if (!expected.isBlank()) {

            String[] keywords =
                    expected.split("\\s+");

            int matched = 0;
            int totalKeywords = 0;

            for (String keyword : keywords) {

                String clean =
                        keyword.replaceAll(
                                "[^a-zA-Z0-9]",
                                ""
                        );

                if (clean.length() < 4) {
                    continue;
                }

                totalKeywords++;

                if (answer.contains(clean)) {
                    matched++;
                }
            }

            if (totalKeywords > 0) {

                double percentage =
                        ((double) matched /
                                totalKeywords) * 100;

                score =
                        (int) Math.round(percentage);

                if (score > 100) {
                    score = 100;
                }
            }
        }

        String feedback;

        if (score >= 80) {

            feedback =
                    "Good answer. You covered the important concepts and demonstrated a strong understanding.";

        } else if (score >= 60) {

            feedback =
                    "Decent answer. You covered some important concepts, but the explanation could be more complete.";

        } else if (score >= 40) {

            feedback =
                    "Your answer shows some understanding, but several important concepts are missing. Try to explain the topic in more detail.";

        } else {

            feedback =
                    "The answer needs significant improvement. Review the core concepts and provide a clearer and more complete explanation.";
        }

        return new EvaluationResult(
                score,
                feedback
        );
    }

    // =========================================================
    // EXTRACT GEMINI TEXT
    // =========================================================

    private String extractGeneratedText(String json) {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {

            String marker = "\"text\":\"";

            int start =
                    json.indexOf(marker);

            if (start == -1) {
                return null;
            }

            start += marker.length();

            StringBuilder result =
                    new StringBuilder();

            boolean escaped = false;

            for (int i = start;
                 i < json.length();
                 i++) {

                char c =
                        json.charAt(i);

                if (escaped) {

                    if (c == 'n') {
                        result.append('\n');
                    } else if (c == 'r') {
                        result.append('\r');
                    } else if (c == 't') {
                        result.append('\t');
                    } else if (c == '"') {
                        result.append('"');
                    } else if (c == '\\') {
                        result.append('\\');
                    } else {
                        result.append(c);
                    }

                    escaped = false;

                } else if (c == '\\') {

                    escaped = true;

                } else if (c == '"') {

                    break;

                } else {

                    result.append(c);
                }
            }

            return result.toString();

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // EXTRACT SCORE
    // =========================================================

    private int extractScore(String text) {

        try {

            String upper =
                    text.toUpperCase();

            int index =
                    upper.indexOf("SCORE:");

            if (index == -1) {

                Matcher matcher =
                        Pattern.compile(
                                "\\b(100|[1-9]?\\d)\\b"
                        ).matcher(text);

                if (matcher.find()) {

                    int score =
                            Integer.parseInt(
                                    matcher.group(1)
                            );

                    return Math.min(
                            100,
                            Math.max(0, score)
                    );
                }

                return 50;
            }

            String scoreText =
                    text.substring(index + 6).trim();

            Matcher matcher =
                    Pattern.compile("\\d+")
                            .matcher(scoreText);

            if (matcher.find()) {

                int score =
                        Integer.parseInt(
                                matcher.group()
                        );

                return Math.min(
                        100,
                        Math.max(0, score)
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Could not extract score: " +
                            e.getMessage()
            );
        }

        return 50;
    }

    // =========================================================
    // EXTRACT FEEDBACK
    // =========================================================

    private String extractFeedback(String text) {

        try {

            String upper =
                    text.toUpperCase();

            int index =
                    upper.indexOf("FEEDBACK:");

            if (index != -1) {

                String feedback =
                        text.substring(index + 9).trim();

                if (!feedback.isBlank()) {
                    return feedback;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Could not extract feedback: " +
                            e.getMessage()
            );
        }

        return text;
    }

    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private String escapeJson(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    // =========================================================
    // RESULT CLASS
    // =========================================================

    public static class EvaluationResult {

        private final int score;
        private final String feedback;

        public EvaluationResult(
                int score,
                String feedback
        ) {
            this.score = score;
            this.feedback = feedback;
        }

        public int getScore() {
            return score;
        }

        public String getFeedback() {
            return feedback;
        }
    }
}