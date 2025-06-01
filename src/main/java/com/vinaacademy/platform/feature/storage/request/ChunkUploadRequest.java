package com.vinaacademy.platform.feature.storage.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ChunkUploadRequest {
    private UUID sessionId;
    private Integer chunkNumber;
    private String chunkHash; // Optional: for chunk integrity verification
}