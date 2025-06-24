package com.vinaacademy.platform.feature.review.controller;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.feature.common.exception.ResourceNotFoundException;
import com.vinaacademy.platform.feature.common.response.ApiResponse;
import com.vinaacademy.platform.feature.review.dto.CourseReviewDto;
import com.vinaacademy.platform.feature.review.dto.CourseReviewRequestDto;
import com.vinaacademy.platform.feature.review.service.CourseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import vn.vinaacademy.common.security.SecurityContextHelper;
import vn.vinaacademy.common.security.annotation.HasAnyRole;
import vn.vinaacademy.common.constant.AuthConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course-reviews")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Course Review API", description = "API đánh giá khóa học")
public class CourseReviewController {
    private final CourseReviewService courseReviewService;
    @Autowired
    private UserClient userClient;

    @Autowired
    private SecurityContextHelper securityHelper;

    @Operation(summary = "Tạo hoặc cập nhật đánh giá khóa học")
    @HasAnyRole({AuthConstants.STUDENT_ROLE})
    @PostMapping
    public ResponseEntity<ApiResponse<CourseReviewDto>> createOrUpdateReview(
            @Valid @RequestBody CourseReviewRequestDto requestDto) {

        UUID userId = securityHelper.getCurrentUserIdAsUUID();

        // Kiểm tra người dùng đã đăng ký khóa học chưa
        if (!courseReviewService.isUserEnrolledInCourse(userId, requestDto.getCourseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>("error", "Bạn chưa đăng ký khóa học này nên không thể đánh giá", null));
        }

        CourseReviewDto reviewDto = courseReviewService.createOrUpdateReview(userId, requestDto);

        log.info("User {} created/updated review for course {}: {}", userId, requestDto.getCourseId(), reviewDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("success", "Đánh giá khóa học thành công", reviewDto));
    }

    @Operation(summary = "Lấy danh sách đánh giá của một khóa học")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<Page<CourseReviewDto>>> getCourseReviews(
            @PathVariable UUID courseId,
            @PageableDefault(size = 10, sort = "createdDate") Pageable pageable) {

        Page<CourseReviewDto> reviews = courseReviewService.getCourseReviews(courseId, pageable);

        log.info("Get {} reviews for course {}", reviews.getTotalElements(), courseId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy danh sách đánh giá thành công", reviews));
    }

    @Operation(summary = "Lấy danh sách đánh giá của người dùng hiện tại")
    @HasAnyRole({AuthConstants.STUDENT_ROLE})
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<CourseReviewDto>>> getUserReviews() {

        UUID userId = securityHelper.getCurrentUserIdAsUUID();
        List<CourseReviewDto> reviews = courseReviewService.getUserReviews(userId);

        log.info("User {} retrieved their reviews: {}", userId, reviews);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy danh sách đánh giá của bạn thành công", reviews));
    }

    @Operation(summary = "Lấy chi tiết một đánh giá")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<CourseReviewDto>> getReviewById(
            @PathVariable Long reviewId) {

        CourseReviewDto review = courseReviewService.getReviewById(reviewId);

        log.info("Get review {}: {}", reviewId, review);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy thông tin đánh giá thành công", review));
    }

    @Operation(summary = "Lấy đánh giá của người dùng hiện tại cho một khóa học")
    @HasAnyRole({AuthConstants.STUDENT_ROLE})
    @GetMapping("/user/course/{courseId}")
    public ResponseEntity<ApiResponse<CourseReviewDto>> getUserReviewForCourse(
            @PathVariable UUID courseId) {

        UUID userId = securityHelper.getCurrentUserIdAsUUID();
        CourseReviewDto review = courseReviewService.getUserReviewForCourse(userId, courseId);

        log.info("User {} retrieved their review for course {}: {}", userId, courseId, review);

        if (review == null) {
            return ResponseEntity.ok(new ApiResponse<>("success", "Bạn chưa đánh giá khóa học này", null));
        }

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy đánh giá của bạn thành công", review));
    }

    @Operation(summary = "Xóa đánh giá")
    @HasAnyRole({AuthConstants.STUDENT_ROLE})
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId) {

        UUID userId = securityHelper.getCurrentUserIdAsUUID();

        //Kiểm tra quyền
        if (!courseReviewService.isReviewOwnedByUser(reviewId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>("error", "Bạn không có quyền xóa đánh giá này", null));
        }
        courseReviewService.deleteReview(userId, reviewId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Xóa đánh giá thành công", null));
    }

    @Operation(summary = "Lấy thống kê đánh giá của một khóa học")
    @GetMapping("/statistics/course/{courseId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseReviewStatistics(
            @PathVariable UUID courseId) {

        Map<String, Object> statistics = courseReviewService.getCourseReviewStatistics(courseId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy thống kê đánh giá thành công", statistics));
    }

    @Operation(summary = "Kiểm tra người dùng đã đánh giá khóa học chưa")
    @HasAnyRole({AuthConstants.STUDENT_ROLE})
    @GetMapping("/check/course/{courseId}")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedCourse(
            @PathVariable UUID courseId) {

        UUID userId = securityHelper.getCurrentUserIdAsUUID();
        boolean hasReviewed = courseReviewService.hasUserReviewedCourse(userId, courseId);

        return ResponseEntity.ok(new ApiResponse<>("success",
                hasReviewed ? "Bạn đã đánh giá khóa học này" : "Bạn chưa đánh giá khóa học này",
                hasReviewed));
    }

}