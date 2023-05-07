package ru.nsu.anikolotov.crackhash.manager.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.anikolotov.crackhash.manager.entity.TaskEntity;

import java.util.UUID;

@Repository
public interface TaskRepository extends MongoRepository<TaskEntity, UUID> {

}
