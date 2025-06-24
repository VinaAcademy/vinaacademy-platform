package com.vinaacademy.platform.feature.enrollment.mapper;

import com.vinaacademy.platform.feature.enrollment.Enrollment;
import com.vinaacademy.platform.feature.enrollment.dto.StudentProgressDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface StudentProgressMapper {
    StudentProgressMapper INSTANCE = Mappers.getMapper(StudentProgressMapper.class);

    @Mapping(source = "userId", target = "studentId")
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "studentEmail", ignore = true)
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.name", target = "courseName")
    @Mapping(source = "progressPercentage", target = "progress")
    @Mapping(source = "status", target = "status")
    StudentProgressDto toDto(Enrollment enrollment);
}