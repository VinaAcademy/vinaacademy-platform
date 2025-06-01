package com.vinaacademy.platform.feature.storage.mapper;

import com.vinaacademy.platform.feature.storage.dto.UploadSessionDto;
import com.vinaacademy.platform.feature.storage.entity.UploadSession;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UploadSessionMapper {
    UploadSessionMapper INSTANCE = Mappers.getMapper(UploadSessionMapper.class);

    UploadSessionDto toDto(UploadSession uploadSession);

    List<UploadSessionDto> toDtoList(List<UploadSession> activeSessions);
}
