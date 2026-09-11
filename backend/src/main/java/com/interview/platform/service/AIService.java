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
                    "Use this scoring rubric strictly:\n" +
                    "90-100 = technically correct and complete, covering nearly all essential concepts.\n" +
                    "75-89 = mostly correct with only minor omissions.\n" +
                    "50-74 = partially correct, with important concepts missing.\n" +
                    "20-49 = weak answer with major missing concepts or misunderstandings.\n" +
                    "0-19 = incorrect, meaningless, unrelated, or non-responsive.\n" +
                    "Judge technical meaning and concept coverage, not grammar, wording style, or answer length.\n" +
                    "A concise but technically correct answer can receive a high score.\n" +
                    "Do not give a high score merely because the answer mentions Java or a few related terms.\n" +
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
            int conceptScore =
                    calculateConceptCoverage(
                            question,
                            candidateAnswer
                    );

            score =
                    calibrateGeminiScore(
                            score,
                            conceptScore
                    );

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

        private int calculateConceptCoverage(
            Question question,
            String answerText
    ) {

        if (question == null ||
                question.getExpectedAnswer() == null ||
                question.getExpectedAnswer().isBlank() ||
                answerText == null ||
                answerText.isBlank()) {

            return 0;
        }

        java.util.Set<String> expectedConcepts =
                extractScoringConcepts(
                        question.getExpectedAnswer()
                );

        java.util.Set<String> answerConcepts =
                extractScoringConcepts(
                        answerText
                );

        if (expectedConcepts.isEmpty()) {
            return 0;
        }

        int matched = 0;

        for (String expectedConcept :
                expectedConcepts) {

            if (containsMatchingConcept(
                    answerConcepts,
                    expectedConcept
            )) {
                matched++;
            }
        }

        double coverage =
                (double) matched /
                        expectedConcepts.size();

        return Math.max(
                0,
                Math.min(
                        100,
                        (int) Math.round(
                                coverage * 100
                        )
                )
        );
    }

    private int calibrateGeminiScore(
            int aiScore,
            int conceptScore
    ) {

        aiScore =
                Math.max(
                        0,
                        Math.min(100, aiScore)
                );

        conceptScore =
                Math.max(
                        0,
                        Math.min(100, conceptScore)
                );

        /*
         * Very low concept coverage means Gemini must not
         * return an unrealistically high mark.
         */
        if (conceptScore < 15) {
            return Math.min(aiScore, 20);
        }

        if (conceptScore < 30) {
            return Math.min(aiScore, 40);
        }

        if (conceptScore < 45) {
            return Math.min(aiScore, 60);
        }

        /*
         * Strong concept coverage protects concise correct
         * answers from being scored too harshly.
         */
        if (conceptScore >= 70) {
            return Math.max(aiScore, 85);
        }

        if (conceptScore >= 55) {
            return Math.max(aiScore, 75);
        }

        return aiScore;
    }

    private int fallbackScoreFromCoverage(
            int conceptScore
    ) {

        if (conceptScore >= 70) {
            return 90;
        }

        if (conceptScore >= 55) {
            return 80;
        }

        if (conceptScore >= 40) {
            return 65;
        }

        if (conceptScore >= 25) {
            return 45;
        }

        if (conceptScore >= 10) {
            return 20;
        }

        return 5;
    }

    private java.util.Set<String> extractScoringConcepts(
            String text
    ) {

        java.util.Set<String> result =
                new java.util.HashSet<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String normalized =
                normalizeText(text);

        for (String raw :
                normalized.split("\\s+")) {

            String word =
                    normalizeScoringWord(raw);

            if (word.length() < 3) {
                continue;
            }

            if (isScoringStopWord(word)) {
                continue;
            }

            result.add(word);
        }

        return result;
    }

    private boolean containsMatchingConcept(
            java.util.Set<String> answerConcepts,
            String expectedConcept
    ) {

        if (answerConcepts.contains(expectedConcept)) {
            return true;
        }

        for (String candidate : answerConcepts) {

            if (candidate == null ||
                    expectedConcept == null) {
                continue;
            }

            if (candidate.length() < 4 ||
                    expectedConcept.length() < 4) {
                continue;
            }

            int distance =
                    levenshteinDistance(
                            candidate,
                            expectedConcept
                    );

            int maxLength =
                    Math.max(
                            candidate.length(),
                            expectedConcept.length()
                    );

            double similarity =
                    1.0 -
                            ((double) distance /
                                    maxLength);

            /*
             * Allows small human spelling mistakes such as:
             * encapsulation -> encapluation
             * inheritance   -> inheratence
             * polymorphism  -> polymorphysim
             */
            if (distance <= 2 &&
                    similarity >= 0.72) {

                return true;
            }
        }

        return false;
    }

    private int levenshteinDistance(
            String first,
            String second
    ) {

        int[] previous =
                new int[second.length() + 1];

        int[] current =
                new int[second.length() + 1];

        for (int j = 0;
             j <= second.length();
             j++) {

            previous[j] = j;
        }

        for (int i = 1;
             i <= first.length();
             i++) {

            current[0] = i;

            for (int j = 1;
                 j <= second.length();
                 j++) {

                int cost =
                        first.charAt(i - 1) ==
                                second.charAt(j - 1)
                                ? 0
                                : 1;

                current[j] =
                        Math.min(
                                Math.min(
                                        current[j - 1] + 1,
                                        previous[j] + 1
                                ),
                                previous[j - 1] + cost
                        );
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[second.length()];
    }

    private String normalizeScoringWord(
            String word
    ) {

        if (word == null) {
            return "";
        }

        String value =
                word.toLowerCase().trim();

        if (value.length() > 6 &&
                value.endsWith("ing")) {

            value =
                    value.substring(
                            0,
                            value.length() - 3
                    );
        }

        if (value.length() > 5 &&
                value.endsWith("ed")) {

            value =
                    value.substring(
                            0,
                            value.length() - 2
                    );
        }

        if (value.length() > 5 &&
                value.endsWith("s") &&
                !value.endsWith("ss")) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value;
    }

    private boolean isScoringStopWord(
            String word
    ) {

        String stopWords =
                "a an the and or but is are was were be been being " +
                "to of in on at for from by with as this that these those " +
                "it its into which what who how when while than then " +
                "can could would should will may might " +
                "use used using provides provide provided required " +
                "allows allow include includes including main generally four pillar pillars";

        return java.util.Arrays
                .asList(
                        stopWords.split("\\s+")
                )
                .contains(word);
    }

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

        int score =
                calculateConceptCoverage(
                        question,
                        answerText
                );

        score =
                fallbackScoreFromCoverage(
                        score
                );

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