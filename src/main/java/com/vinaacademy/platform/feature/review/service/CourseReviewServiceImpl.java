package com.vinaacademy.platform.feature.review.service;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.client.dto.UserDto;
import com.vinaacademy.platform.exception.UnauthorizedException;
import com.vinaacademy.platform.feature.common.exception.ResourceNotFoundException;
import com.vinaacademy.platform.feature.course.entity.Course;
import com.vinaacademy.platform.feature.course.repository.CourseRepository;
import com.vinaacademy.platform.feature.enrollment.repository.EnrollmentRepository;
import com.vinaacademy.platform.feature.review.dto.CourseReviewDto;
import com.vinaacademy.platform.feature.review.dto.CourseReviewRequestDto;
import com.vinaacademy.platform.feature.review.entity.CourseReview;
import com.vinaacademy.platform.feature.review.mapper.CourseReviewMapper;
import com.vinaacademy.platform.feature.review.repository.CourseReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseReviewServiceImpl implements CourseReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final UserClient userClient;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public CourseReviewDto createOrUpdateReview(UUID userId, CourseReviewRequestDto requestDto) {
        UserDto user = userClient.getUserByIdAsDto(userId).getData();

        Course course = courseRepository.findById(requestDto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + requestDto.getCourseId()));

        // Kiểm tra người dùng đã đăng ký khóa học chưa
        if (!isUserEnrolledInCourse(userId, course.getId())) {
            throw new UnauthorizedException();
        }

        // Kiểm tra xem người dùng đã đánh giá khóa học này chưa
        CourseReview courseReview = courseReviewRepository.findByCourseIdAndUserId(course.getId(), userId)
                .orElse(null);

        if (courseReview == null) {
            // Tạo đánh giá mới
            courseReview = CourseReviewMapper.INSTANCE.toEntity(requestDto, user, course);
            courseReview = courseReviewRepository.save(courseReview);
        } else {
            // Cập nhật đánh giá hiện có
            CourseReviewMapper.INSTANCE.updateEntityFromDto(requestDto, courseReview);
            courseReview = courseReviewRepository.save(courseReview);
        }

        // Cập nhật đánh giá trung bình của khóa học
        updateCourseAverageRating(course.getId());

        return CourseReviewMapper.INSTANCE.toDto(courseReview, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseReviewDto> getCourseReviews(UUID courseId, Pageable pageable) {
        // Kiểm tra khóa học tồn tại
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId);
        }

        Page<CourseReview> reviewPage = courseReviewRepository.findByCourseId(courseId, pageable);
        return reviewPage.map(CourseReviewMapper.INSTANCE::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseReviewDto> getUserReviews(UUID userId) {
        // Kiểm tra người dùng tồn tại
        UserDto user = userClient.getUserByIdAsDto(userId).getData();

        List<CourseReview> reviews = courseReviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(v -> CourseReviewMapper.INSTANCE.toDto(v, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReviewDto getReviewById(Long reviewId) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));
        UserDto user = userClient.getUserByIdAsDto(review.getUserId()).getData();
        return CourseReviewMapper.INSTANCE.toDto(review, user);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReviewDto getUserReviewForCourse(UUID userId, UUID courseId) {
        return courseReviewRepository.findByCourseIdAndUserId(courseId, userId)
                .map(CourseReviewMapper.INSTANCE::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteReview(UUID userId, Long reviewId) {
        CourseReview review = courseReviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        UUID courseId = review.getCourse().getId();
        courseReviewRepository.delete(review);

        // Cập nhật đánh giá trung bình của khóa học
        updateCourseAverageRating(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCourseReviewStatistics(UUID courseId) {
        // Kiểm tra khóa học tồn tại
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId);
        }

        Map<String, Object> statistics = new HashMap<>();

        // Tính điểm đánh giá trung bình
        Double averageRating = courseReviewRepository.calculateAverageRatingByCourseId(courseId);
        statistics.put("averageRating", averageRating != null ? averageRating : 0.0);

        // Đếm số lượng đánh giá theo điểm (1-5)
        List<Object[]> ratingCounts = courseReviewRepository.countRatingsByCourseId(courseId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();

        // Khởi tạo mặc định là 0 cho tất cả các điểm từ 1-5
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }

        // Cập nhật số lượng thực tế
        for (Object[] result : ratingCounts) {
            Integer rating = ((Number) result[0]).intValue();
            Long count = ((Number) result[1]).longValue();
            ratingDistribution.put(rating, count);
        }

        statistics.put("ratingDistribution", ratingDistribution);

        // Tổng số đánh giá
        long totalReviews = ratingDistribution.values().stream().mapToLong(Long::longValue).sum();
        statistics.put("totalReviews", totalReviews);

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedCourse(UUID userId, UUID courseId) {
        return courseReviewRepository.existsByCourseIdAndUserId(courseId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserEnrolledInCourse(UUID userId, UUID courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public boolean isReviewOwnedByUser(Long reviewId, UUID userId) {
        return courseReviewRepository.existsByIdAndUserId(reviewId, userId);
    }

    private void updateCourseAverageRating(UUID courseId) {
        Double averageRating = courseReviewRepository.calculateAverageRatingByCourseId(courseId);

        if (averageRating != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId));

            course.setRating(averageRating);
            courseRepository.save(course);
        }
    }
}
