package com.vinaacademy.platform.client.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Wrapper entity for User from user-service
 * This is used to maintain compatibility with existing code structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String avatar;
}
