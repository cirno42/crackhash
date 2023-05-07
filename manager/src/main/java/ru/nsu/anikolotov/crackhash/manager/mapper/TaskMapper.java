package ru.nsu.anikolotov.crackhash.manager.mapper;

import org.mapstruct.Mapper;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackingStatusResponse;
import ru.nsu.anikolotov.crackhash.manager.entity.TaskEntity;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    CrackingStatusResponse taskEntityToStatusResponse(TaskEntity entity);
}
