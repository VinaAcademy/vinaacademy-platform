package com.vinaacademy.platform.feature.instructor.repository;

import com.vinaacademy.platform.feature.course.entity.Course;
import com.vinaacademy.platform.feature.instructor.CourseInstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, Long> {
    /**
     * Kiểm tra xem người dùng có phải là giảng viên của bất kỳ khóa học nào không
     *
     * @param instructorId ID của giảng viên
     * @return true nếu là giảng viên của ít nhất một khóa học
     */
    boolean existsByInstructorId(UUID instructorId);

    /**
     * Lấy danh sách ID của các khóa học mà giảng viên dạy
     *
     * @param instructorId ID của giảng viên
     * @return Danh sách ID của các khóa học
     */
    @Query("SELECT ci.course.id FROM CourseInstructor ci WHERE ci.instructorId = :instructorId")
    List<UUID> findCourseIdsByInstructorId(@Param("instructorId") UUID instructorId);
    /**
     * Find a course instructorId by instructorId and course
     *
     * @param instructorId the instructorId
     * @param course the course
     * @return the course instructorId if found
     */
    Optional<CourseInstructor> findByInstructorIdAndCourse(UUID instructorId, Course course);

    /**
     * Find all course instructors by instructorId
     *
     * @param instructorId the instructorId
     * @return list of course instructors
     */
    List<CourseInstructor> findByInstructorId(UUID instructorId);

    /**
     * Find all course instructors by course
     *
     * @param course the course
     * @return list of course instructors
     */
    List<CourseInstructor> findByCourse(Course course);

    /**
     * Find the owner of a course
     *
     * @param course the course
     * @return the course instructor who is the owner
     */
    Optional<CourseInstructor> findByCourseAndIsOwnerTrue(Course course);

    /**
     * Count the number of instructors for a course
     *
     * @param course the course
     * @return the number of instructors
     */
    long countByCourse(Course course);

    /**
     * Check if a user is an instructor of a course
     *
     * @param instructorId the instructor ID
     * @param courseId the course ID
     * @return true if the user is an instructor of the course
     */
    @Query("SELECT CASE WHEN COUNT(ci) > 0 THEN true ELSE false END FROM CourseInstructor ci " +
           "WHERE ci.instructorId = :instructorId AND ci.course.id = :courseId")
    boolean existsByInstructorIdAndCourseId(@Param("instructorId") UUID instructorId, @Param("courseId") UUID courseId);

    boolean existsByCourseIdAndInstructorId(UUID courseId, UUID id);
    
    Long countByInstructorIdAndIsOwnerTrue(UUID instructorId);
}