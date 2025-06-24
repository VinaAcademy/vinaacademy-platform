package com.vinaacademy.platform.feature.course.repository;

import com.vinaacademy.platform.feature.lesson.entity.Lesson;
import com.vinaacademy.platform.feature.lesson.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByLessonAndUserId(Lesson lesson, UUID currentUserId);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId AND up.lesson IN :lessons")
    List<UserProgress> findByUserIdAndLessonIn(@Param("userId") UUID userId, @Param("lessons") List<Lesson> lessons);

    Optional<UserProgress> findByLessonIdAndUserId(UUID lessonId, UUID userId);

    @Query("SELECT COUNT(up) FROM UserProgress up " +
            "WHERE up.userId = :userId AND up.lesson.section.course.id = :courseId " +
            "AND up.completed = true")
    long countCompletedByUserIdAndCourseId(UUID userId, UUID courseId);

    @Query("SELECT up FROM UserProgress up " +
            "WHERE up.userId = :userId AND up.lesson.section.course.id = :courseId")
    List<UserProgress> findLessonProgressByCourseUser(UUID courseId, UUID userId);
}
