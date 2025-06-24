package com.vinaacademy.platform.client.service;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.client.dto.UserDto;
import com.vinaacademy.platform.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserClient userClient;
    
    public UserDto getUserById(UUID userId) {
        try {
            return userClient.getUserByIdAsDto(userId).getData();
        } catch (Exception e) {
            throw BadRequestException.message("Không tìm thấy ID người dùng này");
        }
    }
}
