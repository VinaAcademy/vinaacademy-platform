package com.vinaacademy.platform.feature.storage.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiateUploadRequest {
    @NotBlank(message = "Tên tệp không được để trống")
    private String filename;
    @NotNull(message = "Kích thước tệp không được để trống")
    @Min(value = 1, message = "Kích thước tệp phải lớn hơn 0")
    private Long fileSize;
    private String fileHash; // Optional: for resume detection
    private UUID lessonId;
    @NotNull(message = "Kích thước phân đoạn không được để trống")
    @Min(value = 1, message = "Kích thước phân đoạn phải lớn hơn 0")
    private Integer chunkSize; // Default: 1MB
}