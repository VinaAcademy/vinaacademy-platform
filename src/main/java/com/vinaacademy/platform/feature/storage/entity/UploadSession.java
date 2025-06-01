package com.vinaacademy.platform.feature.storage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(name = "upload_sessions")
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;

    @Column(name = "uploaded_chunks", nullable = false)
    private int uploadedChunks;

    @Column(name = "file_hash", nullable = false)
    private String fileHash;

    @Column(name = "temp_file_path", nullable = false)
    private String tempFilePath;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusDays(1); // Default expiration time of 1 day
        this.status = UploadStatus.INITIATED; // Default status
    }

    public enum UploadStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        EXPIRED
    }
}
