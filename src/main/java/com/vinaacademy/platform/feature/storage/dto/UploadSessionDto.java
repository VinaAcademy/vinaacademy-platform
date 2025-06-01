package com.vinaacademy.platform.feature.storage.dto;

import com.vinaacademy.platform.feature.storage.entity.UploadSession;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UploadSessionDto implements UploadResult {
    private UUID sessionId;
    private String filename;
    private Long fileSize;
    private Integer chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private UploadSession.UploadStatus status;
    private LocalDateTime expiresAt;
    private Double progressPercentage;
}