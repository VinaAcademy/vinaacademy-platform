package com.vinaacademy.platform.feature.video.mapper;

import com.vinaacademy.platform.feature.video.dto.VideoNoteDto;
import com.vinaacademy.platform.feature.video.dto.VideoNoteRequestDto;
import com.vinaacademy.platform.feature.video.entity.Video;
import com.vinaacademy.platform.feature.video.entity.VideoNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper
public interface VideoNoteMapper {
    VideoNoteMapper INSTANCE = Mappers.getMapper(VideoNoteMapper.class);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "videoId", source = "video.id")
    VideoNoteDto toDto(VideoNote videoNote);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "video", source = "video")
    VideoNote toEntity(VideoNoteRequestDto requestDto, UUID userId, Video video);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "video", ignore = true)
    void updateEntityFromDto(VideoNoteRequestDto requestDto, @MappingTarget VideoNote videoNote);
}
