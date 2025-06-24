package com.vinaacademy.platform.client;

import com.vinaacademy.platform.client.dto.UserDto;
import com.vinaacademy.platform.feature.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.vinaacademy.common.constant.ServiceNames;

import java.util.UUID;

@FeignClient(name = ServiceNames.USER_SERVICE, path = "/api/v1/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    ApiResponse<UserDto> getUserByIdAsDto(@PathVariable("userId") UUID userId);
}
