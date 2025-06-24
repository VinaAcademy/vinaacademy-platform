package com.vinaacademy.platform.feature.video.controller;

import com.vinaacademy.platform.feature.common.response.ApiResponse;
import com.vinaacademy.platform.feature.video.dto.VideoNoteDto;
import com.vinaacademy.platform.feature.video.dto.VideoNoteRequestDto;
import com.vinaacademy.platform.feature.video.service.VideoNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video-notes")
@Tag(name = "Video Note API", description = "API quản lý ghi chú trên video")
public class VideoNoteController {
    @Autowired
    private VideoNoteService videoNoteService;
    @Autowired
    private SecurityContextHelper securityHelper;

    @Operation(summary = "Tạo ghi chú mới cho video")
    @PostMapping
    public ResponseEntity<ApiResponse<VideoNoteDto>> createVideoNote(
            @Valid @RequestBody VideoNoteRequestDto requestDto) {

        // Lấy đối tượng User hiện tại
        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();

        // Gọi service với đối tượng User
        VideoNoteDto createdNote = videoNoteService.createVideoNote(currentUserId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("success", "Tạo ghi chú thành công", createdNote));
    }

    @Operation(summary = "Cập nhật thông tin ghi chú")
    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<VideoNoteDto>> updateVideoNote(
            @PathVariable Long noteId,
            @Valid @RequestBody VideoNoteRequestDto requestDto) {

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        VideoNoteDto updatedNote = videoNoteService.updateVideoNote(currentUserId, noteId, requestDto);

        return ResponseEntity.ok(new ApiResponse<>("success", "Cập nhật ghi chú thành công", updatedNote));
    }

    @Operation(summary = "Lấy tất cả ghi chú của người dùng cho một video cụ thể")
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<VideoNoteDto>>> getVideoNotesByVideo(
            @PathVariable UUID videoId) {

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        List<VideoNoteDto> videoNotes = videoNoteService.getVideoNotesByVideoAndUser(currentUserId, videoId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy danh sách ghi chú thành công", videoNotes));
    }

    @Operation(summary = "Lấy tất cả ghi chú của người dùng")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoNoteDto>>> getAllVideoNotes() {

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        List<VideoNoteDto> videoNotes = videoNoteService.getAllVideoNotesByUser(currentUserId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy danh sách ghi chú thành công", videoNotes));
    }

    @Operation(summary = "Lấy thông tin chi tiết của một ghi chú")
    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<VideoNoteDto>> getVideoNoteById(
            @PathVariable Long noteId) {

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        VideoNoteDto videoNote = videoNoteService.getVideoNoteById(currentUserId, noteId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Lấy thông tin ghi chú thành công", videoNote));
    }

    @Operation(summary = "Xóa một ghi chú")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteVideoNote(
            @PathVariable Long noteId) {

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        videoNoteService.deleteVideoNote(currentUserId, noteId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Xóa ghi chú thành công", null));
    }

}
