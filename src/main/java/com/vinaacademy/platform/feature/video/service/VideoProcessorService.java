package com.vinaacademy.platform.feature.video.service;

import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.email.service.EmailService;
import com.vinaacademy.platform.feature.notification.dto.NotificationCreateDTO;
import com.vinaacademy.platform.feature.notification.service.NotificationService;
import com.vinaacademy.platform.feature.storage.properties.StorageProperties;
import com.vinaacademy.platform.feature.video.entity.Video;
import com.vinaacademy.platform.feature.video.enums.VideoStatus;
import com.vinaacademy.platform.feature.video.repository.VideoRepository;
import com.vinaacademy.platform.feature.video.utils.FFmpegUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessorService {

    private final VideoRepository videoRepository;
    private final NotificationService notificationService;
    private final StorageProperties storageProperties;

    @Value("${application.url.frontend}")
    private String frontendUrl;

    @Async("videoTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processVideo(UUID videoId, Path inputFile) {
        Video video = videoRepository.findByIdWithLock(videoId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy video"));
        Path outputDir = Paths.get(storageProperties.getHlsDir(), videoId.toString());
        Path thumbnailPath = Paths.get(storageProperties.getThumbnailDir(), videoId + ".jpg");

        try {
            int exitCode = FFmpegUtils.convertToAdaptiveHLS(inputFile, outputDir, thumbnailPath);
            if (exitCode != 0) throw new RuntimeException("FFmpeg exited with code " + exitCode);

            updateVideoSuccess(video, outputDir, thumbnailPath, inputFile);
            notifySuccess(video);
            log.info("✅ Video {} processed successfully.", videoId);

        } catch (Exception e) {
            log.error("❌ Error processing video {}: {}", videoId, e.getMessage(), e);
            updateVideoFailure(video);
            notifyFailure(video, e.getMessage());
        }

        videoRepository.save(video);
    }

    private Video getVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy video"));
        if (VideoStatus.PROCESSING.equals(video.getStatus())) {
            throw BadRequestException.message("Video đang được xử lý");
        }
        return video;
    }

    private void updateVideoSuccess(Video video, Path hlsPath, Path thumbnailPath, Path inputFile) throws IOException, InterruptedException {
        video.setStatus(VideoStatus.READY);
        video.setHlsPath(hlsPath.toString());
        video.setDuration(FFmpegUtils.getVideoDurationInSeconds(inputFile));
        if (video.getThumbnailUrl() == null) {
            video.setThumbnailUrl(thumbnailPath.toString());
        }
    }

    private void updateVideoFailure(Video video) {
        video.setStatus(VideoStatus.ERROR);
    }

    private void notifySuccess(Video video) {
        String courseId = video.getSection().getCourse().getId().toString();
        notificationService.createNotification(NotificationCreateDTO.builder()
                .title("Video " + video.getTitle() + " đã được xử lý thành công")
                .content("Bạn có thể xem tại đây.")
                .targetUrl(frontendUrl + "/instructor/courses/" + courseId + "/lectures/" + video.getId())
                .userId(video.getAuthor().getId())
                .build());
    }

    private void notifyFailure(Video video, String errorMessage) {
        String courseId = video.getSection().getCourse().getId().toString();
        notificationService.createNotification(NotificationCreateDTO.builder()
                .title("Lỗi xử lý video " + video.getTitle())
                .content("Có lỗi xảy ra: " + errorMessage)
                .targetUrl(frontendUrl + "/instructor/courses/" + courseId + "/lectures/" + video.getId())
                .userId(video.getAuthor().getId())
                .build());
    }
}
