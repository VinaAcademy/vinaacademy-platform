package com.vinaacademy.platform.feature.lesson.service.impl;

import com.vinaacademy.platform.feature.course.repository.UserProgressRepository;
import com.vinaacademy.platform.feature.lesson.dto.LessonProgressDto;
import com.vinaacademy.platform.feature.lesson.mapper.LessonProgressMapper;
import com.vinaacademy.platform.feature.lesson.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {
    private final UserProgressRepository userProgressRepository;

    @Autowired private SecurityContextHelper securityHelper;

    @Override
    public List<LessonProgressDto> getAllLessonProgressByCourse(UUID courseId) {
        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        return userProgressRepository.findLessonProgressByCourseUser(courseId, currentUserId)
                .stream()
                .map(LessonProgressMapper.INSTANCE::toDto)
                .toList();
    }
}
