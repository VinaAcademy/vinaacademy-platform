package com.vinaacademy.platform.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private String avatarUrl;
    private String description;
    private boolean isCollaborator;
    private LocalDate birthday;
    private Set<Role> roles = new HashSet<>();
    private boolean isActive;
}
