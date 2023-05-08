package ru.nsu.anikolotov.crackhash.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import ru.nsu.anikolotov.crackhash.manager.enums.CrackingStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TaskEntity {
    @Id
    private UUID uuid;
    private CrackingStatus status;
    private String hash;
    private int maxLength;
    private List<String> data;
    private List<Integer> finishedWorkers;
    private Instant startTime;

    public TaskEntity(UUID uuid, String hash, int maxLength) {
        this.uuid = uuid;
        this.startTime = Instant.now();
        this.status = CrackingStatus.IN_PROGRESS;
        this.hash = hash;
        this.maxLength = maxLength;
        this.data = new ArrayList<>();
        this.finishedWorkers = new ArrayList<>();
    }
}
