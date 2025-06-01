package com.vinaacademy.platform.feature.storage.repository;

import com.vinaacademy.platform.feature.storage.entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    List<UploadSession> findByUserIdAndStatus(UUID userId, UploadSession.UploadStatus status);

    Optional<UploadSession> findByFileHashAndUserIdAndStatus(String fileHash, UUID userId, UploadSession.UploadStatus status);
}
