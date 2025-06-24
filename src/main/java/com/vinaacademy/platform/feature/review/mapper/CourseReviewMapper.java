package com.vinaacademy.platform.feature.review.mapper;

import com.vinaacademy.platform.client.dto.UserDto;
import com.vinaacademy.platform.feature.course.entity.Course;
import com.vinaacademy.platform.feature.review.dto.CourseReviewDto;
import com.vinaacademy.platform.feature.review.dto.CourseReviewRequestDto;
import com.vinaacademy.platform.feature.review.entity.CourseReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CourseReviewMapper {
    CourseReviewMapper INSTANCE = Mappers.getMapper(CourseReviewMapper.class);

    @Mapping(target = "id", source = "courseReview.id")
    @Mapping(target = "courseId", source = "courseReview.course.id")
    @Mapping(target = "courseName", source = "courseReview.course.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullName")
    CourseReviewDto toDto(CourseReview courseReview, UserDto user);

    @Mapping(target = "id", source = "courseReview.id")
    @Mapping(target = "courseId", source = "courseReview.course.id")
    @Mapping(target = "courseName", source = "courseReview.course.name")
    @Mapping(target = "userId", source = "userId")
    CourseReviewDto toDto(CourseReview courseReview);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", source = "course")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "rating", source = "requestDto.rating")
    CourseReview toEntity(CourseReviewRequestDto requestDto, UserDto user, Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void updateEntityFromDto(CourseReviewRequestDto requestDto, @MappingTarget CourseReview courseReview);
}