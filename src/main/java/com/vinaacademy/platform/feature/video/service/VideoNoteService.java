package com.vinaacademy.platform.feature.video.service;

import com.vinaacademy.platform.feature.video.dto.VideoNoteDto;
import com.vinaacademy.platform.feature.video.dto.VideoNoteRequestDto;

import java.util.List;
import java.util.UUID;

public interface VideoNoteService {
    //Tạo ghi chú mới
    VideoNoteDto createVideoNote(UUID userId, VideoNoteRequestDto requestDto);

    //Cập nhật ghi chú
    VideoNoteDto updateVideoNote(UUID userId, Long noteId, VideoNoteRequestDto requestDto);

    //Lấy tất cả ghi chú của một người dùng cho một video cụ thể
    List<VideoNoteDto> getVideoNotesByVideoAndUser(UUID userId, UUID videoId);

    //Lấy tất cả ghi chú của một người dùng
    List<VideoNoteDto> getAllVideoNotesByUser(UUID userId);

    //Lấy thông tin chi tiết của một ghi chú
    VideoNoteDto getVideoNoteById(UUID userId, Long noteId);

    //Xóa một ghi chú
    void deleteVideoNote(UUID userId, Long noteId);

}
