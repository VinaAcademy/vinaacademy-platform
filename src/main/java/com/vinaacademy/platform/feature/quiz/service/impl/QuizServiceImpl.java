package com.vinaacademy.platform.feature.quiz.service.impl;

import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.exception.NotFoundException;
import com.vinaacademy.platform.exception.ValidationException;
import com.vinaacademy.platform.feature.course.repository.UserProgressRepository;
import com.vinaacademy.platform.feature.lesson.entity.UserProgress;
import com.vinaacademy.platform.feature.lesson.service.LessonService;
import com.vinaacademy.platform.feature.quiz.dto.*;
import com.vinaacademy.platform.feature.quiz.entity.*;
import com.vinaacademy.platform.feature.quiz.enums.QuestionType;
import com.vinaacademy.platform.feature.quiz.mapper.QuizMapper;
import com.vinaacademy.platform.feature.quiz.repository.AnswerRepository;
import com.vinaacademy.platform.feature.quiz.repository.QuestionRepository;
import com.vinaacademy.platform.feature.quiz.repository.QuizRepository;
import com.vinaacademy.platform.feature.quiz.repository.QuizSessionRepository;
import com.vinaacademy.platform.feature.quiz.repository.QuizSubmissionRepository;
import com.vinaacademy.platform.feature.quiz.service.QuizCacheService;
import com.vinaacademy.platform.feature.quiz.service.QuizService;
import com.vinaacademy.platform.feature.quiz.service.QuizSessionService;
import com.vinaacademy.platform.feature.section.entity.Section;
import com.vinaacademy.platform.feature.section.repository.SectionRepository;
import com.vinaacademy.platform.feature.user.auth.annotation.RequiresResourcePermission;
import com.vinaacademy.platform.feature.user.auth.helpers.SecurityHelper;
import com.vinaacademy.platform.feature.user.constant.ResourceConstants;
import com.vinaacademy.platform.feature.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final SectionRepository sectionRepository;
    private final UserProgressRepository userProgressRepository;

    private final QuizCacheService quizCacheService;
    private final QuizSessionService quizSessionService;
    private final LessonService lessonService;

    @Autowired
    private QuizMapper quizMapper;
    @Autowired
    private SecurityHelper securityHelper;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TaskScheduler taskScheduler;

    @Override
    @Transactional(readOnly = true)
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.VIEW_OWN)
    public QuizDto getQuizByIdForInstructor(UUID id) {
        User user = securityHelper.getCurrentUser();
        if (user == null) {
            throw new ValidationException("User not found");
        }

        Quiz quiz = findQuizById(id);
        if (quiz.getSection() == null) {
            throw new ValidationException("Quiz does not belong to any section");
        }

        return quizMapper.quizToQuizDto(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON)
    public QuizDto getQuizForStudent(UUID id) {
        User user = securityHelper.getCurrentUser();
        if (user == null) {
            throw new ValidationException("User not found");
        }

        Quiz quiz = findQuizById(id);
        if (quiz.isRandomizeQuestions()) {
            // Randomize questions for the quiz
            List<Question> questions = questionRepository.findByQuizOrderByCreatedDate(quiz);
            Collections.shuffle(questions);
            quiz.setQuestions(questions);
        }

        List<UUID> questionIds = quiz.getQuestions().stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        List<Answer> allAnswers = answerRepository.findByQuestionIdIn(questionIds);

        Map<UUID, List<Answer>> answersByQuestionId = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));
        for (Question question : quiz.getQuestions()) {
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(),
                    new ArrayList<>());
            Collections.shuffle(answers);
            question.setAnswers(answers);
        }

        return quizMapper.quizToQuizDtoHideCorrectAnswers(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizDto> getQuizzesByCourseId(UUID courseId) {
        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        return quizzes.stream()
                .map(quizMapper::quizToQuizDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizDto> getQuizzesBySectionId(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found with id: " + sectionId));

        List<Quiz> quizzes = quizRepository.findBySectionOrderByOrderIndex(section);
        return quizzes.stream()
                .map(quizMapper::quizToQuizDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.CREATE)
    public QuestionDto createQuestion(UUID quizId, QuestionDto questionDto) {
        Quiz quiz = findQuizById(quizId);

        Question question = Question.builder()
                .quiz(quiz)
                .questionText(questionDto.getQuestionText())
                .explanation(questionDto.getExplanation())
                .point(questionDto.getPoint() != null ? questionDto.getPoint() : 1.0)
                .questionType(questionDto.getQuestionType() != null ? questionDto.getQuestionType() : QuestionType.SINGLE_CHOICE)
                .build();

        Question savedQuestion = questionRepository.save(question);

        // Process associated answers if they exist
        if (questionDto.getAnswers() != null && !questionDto.getAnswers().isEmpty()) {
            questionDto.getAnswers().forEach(answerDto ->
                    createAnswer(savedQuestion.getId(), answerDto));
        }

        // Update quiz total points
        updateQuizTotalPoints(quiz);

        return questionRepository.findById(savedQuestion.getId())
                .map(quizMapper::questionToQuestionDto)
                .orElseThrow(() -> new NotFoundException("Question not found after creation"));
    }

    @Override
    @Transactional
    public QuestionDto updateQuestion(UUID questionId, QuestionDto questionDto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found with id: " + questionId));

        question.setQuestionText(questionDto.getQuestionText());
        question.setExplanation(questionDto.getExplanation());

        if (questionDto.getPoint() != null) {
            question.setPoint(questionDto.getPoint());
        }

        if (questionDto.getQuestionType() != null) {
            updateQuestionType(question, questionDto.getQuestionType());
        }

        Question savedQuestion = questionRepository.save(question);

        // Update quiz total points
        updateQuizTotalPoints(savedQuestion.getQuiz());

        return quizMapper.questionToQuestionDto(savedQuestion);
    }

    private void updateQuestionType(Question question, QuestionType questionType) {
        QuestionType oldType = question.getQuestionType();
        if (oldType == questionType) {
            return;
        }

        question.setQuestionType(questionType);

        // Xóa toàn bộ answer cũ bằng cách thao tác trực tiếp trên collection
        List<Answer> existingAnswers = question.getAnswers();
        boolean willClear = questionType == QuestionType.TRUE_FALSE ||
                questionType == QuestionType.TEXT;
        if (existingAnswers != null && willClear) {
            existingAnswers.clear();
        }

        if (questionType == QuestionType.TRUE_FALSE) {
            Answer trueAnswer = Answer.builder()
                    .answerText("Đúng")
                    .isCorrect(true)
                    .build();

            Answer falseAnswer = Answer.builder()
                    .answerText("Sai")
                    .isCorrect(false)
                    .build();

            question.addAnswer(trueAnswer); // sử dụng helper method đã có
            question.addAnswer(falseAnswer);
        }

        if (questionType == QuestionType.SINGLE_CHOICE) {
            assert question.getAnswers() != null;
            long correctCount = question.getAnswers().stream().filter(Answer::getIsCorrect).count();
            if (correctCount > 1) {
                throw new ValidationException("Câu hỏi loại single choice chỉ được có một đáp án đúng");
            }
        }
    }


    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found with id: " + questionId));

        Quiz quiz = question.getQuiz();
        questionRepository.delete(question);

        // Update quiz total points
        updateQuizTotalPoints(quiz);
    }

    @Override
    @Transactional
    public AnswerDto createAnswer(UUID questionId, AnswerDto answerDto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found with id: " + questionId));

        // For single choice, ensure we don't have multiple correct answers
        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE ||
                question.getQuestionType() == QuestionType.TRUE_FALSE) {

            if (Boolean.TRUE.equals(answerDto.getIsCorrect())) {
                // Check if there's already a correct answer
                boolean hasCorrectAnswer = !answerRepository.findByQuestionIdAndIsCorrect(questionId, true).isEmpty();

                if (hasCorrectAnswer) {
                    throw new ValidationException("Single choice and true/false questions can only have one correct answer");
                }
            }
        }

        Answer answer = Answer.builder()
                .question(question)
                .answerText(answerDto.getAnswerText())
                .isCorrect(answerDto.getIsCorrect() != null ? answerDto.getIsCorrect() : false)
                .build();

        Answer savedAnswer = answerRepository.save(answer);
        return quizMapper.answerToAnswerDto(savedAnswer);
    }

    @Override
    @Transactional
    public AnswerDto updateAnswer(UUID answerId, AnswerDto answerDto) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found with id: " + answerId));

        Question question = answer.getQuestion();

        // Check if we're changing this to be a correct answer in single choice questions
        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE ||
                question.getQuestionType() == QuestionType.TRUE_FALSE) {

            if (Boolean.TRUE.equals(answerDto.getIsCorrect()) && !answer.getIsCorrect()) {
                // If we're making this the correct answer, remove correct flag from other answers
                answerRepository.findByQuestionIdAndIsCorrect(question.getId(), true)
                        .forEach(a -> {
                            a.setIsCorrect(false);
                            answerRepository.save(a);
                        });
            }
        }

        answer.setAnswerText(answerDto.getAnswerText());

        if (answerDto.getIsCorrect() != null) {
            answer.setIsCorrect(answerDto.getIsCorrect());
        }

        Answer savedAnswer = answerRepository.save(answer);
        return quizMapper.answerToAnswerDto(savedAnswer);
    }

    @Override
    @Transactional
    public void deleteAnswer(UUID answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found with id: " + answerId));

        answerRepository.delete(answer);
    }

    @Override
    @Transactional
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.VIEW)
    public QuizSession startQuiz(UUID quizId) {
        User currentUser = securityHelper.getCurrentUser();
        Quiz quiz = findQuizById(quizId);

        // Check if there's an active session already
        Optional<QuizSession> existingSession = findActiveQuizSession(quizId, currentUser.getId());

        if (existingSession.isPresent()) {
            QuizSession session = existingSession.get();

            // If the session has expired but is still marked active, deactivate it
            if (session.isExpired()) {
                this.processSubmitQuiz(quizSessionService.getQuizSubmissionBySession(session),
                        session, session.getStartTime(), LocalDateTime.now());
                quizSessionService.deactivateSession(session);
            } else {
                // Return the existing active session
                return session;
            }
        }

        // Create a new session
        QuizSession session = QuizSession.createNewSession(quiz, currentUser);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(quiz.getTimeLimit());
        session.setExpiryTime(expiryTime);
        taskScheduler.schedule(() -> {
            this.processSubmitQuiz(quizSessionService.getQuizSubmissionBySession(session),
                    session, session.getStartTime(), LocalDateTime.now());
            quizSessionService.deactivateSession(session);
        }, session.getExpiryTime()
                .atZone(ZoneOffset.systemDefault())
                .toInstant());
        return quizSessionRepository.save(session);
    }

    @Override
    @Transactional
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.VIEW,
            idParam = "request.quizId")
    public QuizSubmissionResultDto submitQuiz(QuizSubmissionRequest request) {
        User currentUser = securityHelper.getCurrentUser();
        Quiz quiz = findQuizById(request.getQuizId());

        // Check if we allow retaking quizzes and if the student has already taken this quiz
        validateRetakePolicy(quiz, currentUser.getId());

        // Get current time as end time
        LocalDateTime endTime = LocalDateTime.now();

        // Find and validate the active session
        QuizSession session = validateAndEndActiveSession(request.getQuizId(), currentUser.getId());
        LocalDateTime startTime = session.getStartTime();

        // Validate time limit if quiz has one
        validateTimeLimit(quiz, startTime, endTime);

        // Create new quiz submission
        return processSubmitQuiz(request, session, startTime, endTime);
    }

    private QuizSubmissionResultDto processSubmitQuiz(QuizSubmissionRequest request, QuizSession quizSession, LocalDateTime startTime,
                                                      LocalDateTime endTime) {
        QuizSubmission submission = QuizSubmission.builder()
                .quizSession(quizSession)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Track points and scoring
        Quiz quiz = quizSession.getQuiz();
        User currentUser = quizSession.getUser();

        double totalPoints = quiz.getTotalPoints();
        double earnedPoints = 0;

        // Process each user answer
        for (UserAnswerRequest userAnswerRequest : request.getAnswers()) {
            Question question = questionRepository.findById(userAnswerRequest.getQuestionId())
                    .orElseThrow(() -> new ValidationException("Question not found: " + userAnswerRequest.getQuestionId()));

            UserAnswer userAnswer = createUserAnswer(submission, question, userAnswerRequest);
            submission.addUserAnswer(userAnswer);

            if (userAnswer.isCorrect()) {
                earnedPoints += userAnswer.getEarnedPoints();
            }
        }

        // Calculate final score and pass status
        submission.setTotalPoints(totalPoints);
        submission.setScore(earnedPoints);

        double scorePercentage = (totalPoints > 0) ? (earnedPoints / totalPoints) * 100 : 0;
        submission.setPassed(scorePercentage >= quiz.getPassingScore());

        Optional<UserProgress> upOpt = userProgressRepository
                .findByLessonIdAndUserId(quiz.getId(), currentUser.getId());
        if (submission.isPassed() && upOpt.isEmpty()) {
            lessonService.markLessonCompleted(quiz, currentUser);
        }

        QuizSubmission savedSubmission = quizSubmissionRepository.save(submission);

        quizSession.setQuizSubmission(submission);
        quizSession.setActive(false);
        quizSessionRepository.save(quizSession);

        // Convert to result DTO
        return buildSubmissionResultDto(savedSubmission);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.VIEW)
    public QuizSubmissionResultDto getLatestSubmission(UUID quizId) {
        User currentUser = securityHelper.getCurrentUser();

        Optional<QuizSubmission> latestSubmission = quizSubmissionRepository
                .findFirstByQuizIdAndUserIdOrderByCreatedDateDesc(quizId, currentUser.getId());

        return latestSubmission.map(this::buildSubmissionResultDto).orElse(null);

    }

    @Override
    @Transactional(readOnly = true)
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON,
            permission = ResourceConstants.VIEW)
    public List<QuizSubmissionResultDto> getSubmissionHistory(UUID quizId) {
        User currentUser = securityHelper.getCurrentUser();

        List<QuizSubmission> submissions = quizSubmissionRepository
                .findByQuizIdAndUserIdOrderByCreatedDateDesc(quizId, currentUser.getId());

        if (submissions.isEmpty()) {
            throw new NotFoundException("No submissions found for quiz: " + quizId);
        }

        return submissions.stream()
                .map(this::buildSubmissionResultDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresResourcePermission(resourceType = ResourceConstants.LESSON)
    public List<QuizSubmissionResultDto> getQuizSubmissions(UUID quizId) {
        List<QuizSubmission> submissions = quizSubmissionRepository
                .findByQuizIdAndUserIdOrderByCreatedDateDesc(quizId, null);

        return submissions.stream()
                .map(this::buildSubmissionResultDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void cacheQuizAnswer(UUID quizId, UserAnswerRequest request) {
        User currentUser = securityHelper.getCurrentUser();

        Optional<QuizSession> existingSession = findActiveQuizSession(quizId, currentUser.getId());
        if (existingSession.isEmpty()) {
            throw BadRequestException.message("Không có phiên làm bài nào đang hoạt động");
        }
        QuizSession session = existingSession.get();
        if (session.isExpired()) {
            quizSessionService.deactivateSession(session);
        }
        // Cache the answer using the quiz cache service
        quizCacheService.updateCacheAnswer(currentUser.getId(), session.getId(), quizId, request);
    }

    /**
     * Helper method to find a quiz by ID
     */
    private Quiz findQuizById(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quiz not found with id: " + id));
    }

    /**
     * Helper method to update a quiz's total points based on its questions
     */
    private void updateQuizTotalPoints(Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizOrderByCreatedDate(quiz);

        double totalPoints = questions.stream()
                .mapToDouble(Question::getPoint)
                .sum();

        quiz.setTotalPoints(totalPoints);
        quizRepository.save(quiz);
    }

    /**
     * Create a user answer entity from request
     */
    private UserAnswer createUserAnswer(QuizSubmission submission, Question question, UserAnswerRequest request) {
        UserAnswer userAnswer = UserAnswer.builder()
                .submission(submission)
                .question(question)
                .textAnswer(request.getTextAnswer())
                .build();

        // Process selected answers differently based on question type
        if (question.getQuestionType() == QuestionType.TEXT) {

            // For text-based answers, we just store the text
            // These will need manual grading by instructors
            userAnswer.setCorrect(false); // Not automatically graded
            userAnswer.setEarnedPoints(0); // Will be updated by instructor

        } else {
            // For choice-based questions
            if (request.getSelectedAnswerIds() != null && !request.getSelectedAnswerIds().isEmpty()) {
                List<Answer> selectedAnswers = new ArrayList<>();

                for (UUID answerId : request.getSelectedAnswerIds()) {
                    Answer answer = answerRepository.findById(answerId)
                            .orElseThrow(() -> new ValidationException("Answer not found: " + answerId));

                    selectedAnswers.add(answer);
                }

                userAnswer.setSelectedAnswers(selectedAnswers);

                // Grade the answer based on question type
                boolean isCorrect = evaluateUserAnswer(question, selectedAnswers);
                userAnswer.setCorrect(isCorrect);
                userAnswer.setEarnedPoints(isCorrect ? question.getPoint() : 0);
            } else {
                // No answer selected
                userAnswer.setCorrect(false);
                userAnswer.setEarnedPoints(0);
            }
        }

        return userAnswer;
    }

    /**
     * Evaluate if a user's answer is correct based on question type
     */
    private boolean evaluateUserAnswer(Question question, List<Answer> selectedAnswers) {
        switch (question.getQuestionType()) {
            case SINGLE_CHOICE:
            case TRUE_FALSE:
                // Single choice: exactly one answer should be selected and it should be correct
                if (selectedAnswers.size() != 1) {
                    return false;
                }
                return selectedAnswers.get(0).getIsCorrect();

            case MULTIPLE_CHOICE:
                // Multiple choice: all correct answers must be selected and no incorrect ones
                List<Answer> correctAnswers = answerRepository.findByQuestionIdAndIsCorrect(question.getId(), true);

                // Check if all selected answers are correct
                boolean allSelectedAreCorrect = selectedAnswers.stream()
                        .allMatch(Answer::getIsCorrect);

                // Check if all correct answers are selected
                boolean allCorrectAreSelected = correctAnswers.size() == selectedAnswers.size() &&
                        new HashSet<>(selectedAnswers).containsAll(correctAnswers);

                return allSelectedAreCorrect && allCorrectAreSelected;

            default:
                return false; // For text-based questions that need manual grading
        }
    }

    /**
     * Build a submission result DTO from a submission entity
     */
    private QuizSubmissionResultDto buildSubmissionResultDto(QuizSubmission submission) {
        Quiz quiz = submission.getQuizSession().getQuiz();

        List<UserAnswerResultDto> userAnswerResults = submission.getUserAnswers().stream()
                .map(userAnswer -> {
                    List<AnswerResultDto> answerResults = new ArrayList<>();
                    if (userAnswer.getSelectedAnswers() == null) {
                        userAnswer.setSelectedAnswers(new ArrayList<>());
                    }

                    // Process each answer for the question
                    userAnswer.getQuestion().getAnswers().forEach(answer -> {
                        answerResults.add(AnswerResultDto.builder()
                                .id(answer.getId())
                                .text(answer.getAnswerText())
                                .isSelected(userAnswer.getSelectedAnswers().contains(answer))
                                .isCorrect(quiz.isShowCorrectAnswers() ? answer.getIsCorrect() : null)
                                .build());
                    });

                    return UserAnswerResultDto.builder()
                            .questionId(userAnswer.getQuestion().getId())
                            .questionText(userAnswer.getQuestion().getQuestionText())
                            .explanation(quiz.isShowCorrectAnswers() ? userAnswer.getQuestion().getExplanation() : null)
                            .points(userAnswer.getQuestion().getPoint())
                            .earnedPoints(userAnswer.getEarnedPoints())
                            .isCorrect(userAnswer.isCorrect())
                            .answers(answerResults)
                            .textAnswer(userAnswer.getTextAnswer())
                            .build();
                })
                .collect(Collectors.toList());

        return QuizSubmissionResultDto.builder()
                .id(submission.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .startTime(submission.getStartTime())
                .endTime(submission.getEndTime())
                .score(submission.getScore())
                .totalPoints(submission.getTotalPoints())
                .isPassed(submission.isPassed())
                .answers(userAnswerResults)
                .build();
    }

    /**
     * Helper method to find an active quiz session
     */
    private Optional<QuizSession> findActiveQuizSession(UUID quizId, UUID userId) {
        return quizSessionRepository.findFirstByQuizIdAndUserIdAndActiveTrue(quizId, userId);
    }

    /**
     * Helper method to validate retake policy for a quiz
     */
    private void validateRetakePolicy(Quiz quiz, UUID userId) {
        if (!quiz.isAllowRetake()) {
            boolean hasSubmission = quizSubmissionRepository
                    .findFirstByQuizIdAndUserIdOrderByCreatedDateDesc(quiz.getId(), userId)
                    .isPresent();

            if (hasSubmission) {
                throw new ValidationException("Retaking this quiz is not allowed");
            }
        }
    }

    /**
     * Helper method to validate and end an active session
     */
    private QuizSession validateAndEndActiveSession(UUID quizId, UUID userId) {
        Optional<QuizSession> activeSession = findActiveQuizSession(quizId, userId);

        if (activeSession.isEmpty()) {
            throw new ValidationException("No active quiz session found. Please start the quiz before submitting.");
        }

        QuizSession session = activeSession.get();


        // Mark the session as inactive once submitted
        quizSessionService.deactivateSession(session);

        return session;
    }

    /**
     * Helper method to validate time limit for a quiz
     */
    private void validateTimeLimit(Quiz quiz, LocalDateTime startTime, LocalDateTime endTime) {
        if (quiz.getTimeLimit() > 0) {
            long durationInMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

            if (durationInMinutes > quiz.getTimeLimit()) {
                throw new ValidationException("Time limit exceeded. Quiz time limit is " +
                        quiz.getTimeLimit() + " minutes, but you took " + durationInMinutes + " minutes");
            }
        }
    }
}
