package com.vinaacademy.platform.feature.instructor.service;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.course.entity.Course;
import com.vinaacademy.platform.feature.course.repository.CourseRepository;
import com.vinaacademy.platform.feature.instructor.CourseInstructor;
import com.vinaacademy.platform.feature.instructor.dto.CourseInstructorDto;
import com.vinaacademy.platform.feature.instructor.dto.CourseInstructorDtoRequest;
import com.vinaacademy.platform.feature.instructor.mapper.CourseInstructorMapper;
import com.vinaacademy.platform.feature.instructor.repository.CourseInstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.util.UUID;

import static vn.vinaacademy.common.constant.AuthConstants.ADMIN_ROLE;
import static vn.vinaacademy.common.constant.AuthConstants.STAFF_ROLE;

@Service
@RequiredArgsConstructor
public class CourseInstructorServiceImpl implements CourseInstructorService {

    private final CourseInstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final UserClient userClient;
    @Autowired
    private SecurityContextHelper securityContextHelper;

    @Override
    public CourseInstructorDto createCourseInstructor(CourseInstructorDtoRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy khóa học đó"));

        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        boolean isOwner = instructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUserId);

        if (!isOwner && !securityContextHelper.hasAnyRole(ADMIN_ROLE, STAFF_ROLE)) {
            throw BadRequestException.message("Bạn không phải là người sở hữu khóa học này");
        }

        CourseInstructor instructor = CourseInstructor.builder()
                .course(course)
                .instructorId(request.getUserId())
                .isOwner(request.getIsOwner())
                .build();

        instructorRepository.save(instructor);
        return CourseInstructorMapper.INSTANCE.toDto(instructor);
    }
}
