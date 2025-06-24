package com.vinaacademy.platform.feature.lesson.repository;

import com.vinaacademy.platform.feature.lesson.entity.Lesson;
import com.vinaacademy.platform.feature.lesson.entity.UserProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseUserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserIdAndLesson(UUID userId, Lesson lesson);

    List<UserProgress> findByUserIdAndLessonIn(UUID userId, List<Lesson> lessons);

    List<UserProgress> findByUserId(UUID userId);

    Page<UserProgress> findByLesson(Lesson lesson, Pageable pageable);

    @Query("SELECT up FROM UserProgress up WHERE up.lesson.section.course.id = :courseId")
    Page<UserProgress> findByCourseId(@Param("courseId") UUID courseId, Pageable pageable);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId AND up.lesson.section.course.id = :courseId")
    List<UserProgress> findByUserIdAndCourseId(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.userId = :userId AND up.lesson.section.course.id = :courseId AND up.completed = true")
    long countCompletedLessonsByCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.section.course.id = :courseId")
    long countTotalLessonsByCourse(@Param("courseId") UUID courseId);
}