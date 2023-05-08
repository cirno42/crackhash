package ru.nsu.anikolotov.crackhash.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashManagerRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashWorkerResponse;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackingStatusResponse;
import ru.nsu.anikolotov.crackhash.manager.entity.TaskEntity;
import ru.nsu.anikolotov.crackhash.manager.enums.CrackingStatus;
import ru.nsu.anikolotov.crackhash.manager.mapper.TaskMapper;
import ru.nsu.anikolotov.crackhash.manager.mq.MQConstants;
import ru.nsu.anikolotov.crackhash.manager.repository.TaskRepository;
import ru.nsu.anikolotov.crackhash.manager.utils.Utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {

    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);
    private final TaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TaskMapper taskMapper;

    @Value("${hashcracker.worker-timeout}")
    private Long workerTimeoutInMillis;

    @Value("${hashcracker.workers-amount}")
    private Integer workersAmount;

    public UUID crackHash(CrackHashRequest request) {
        UUID requestUuid = UUID.randomUUID();
        taskRepository.save(new TaskEntity(requestUuid, request.getHash(), request.getMaxLength()));
        sendRequestsToWorkers(requestUuid, request.getHash(), request.getMaxLength());
        return requestUuid;
    }

    public void updateStatus(CrackHashWorkerResponse response) {
        UUID requestId = UUID.fromString(response.getRequestId());
        var currentStatus = taskRepository.findById(requestId)
                .orElseThrow();
        log.info("received answer from {} for task {} result: {}",
                response.getPartNumber(), response.getRequestId(), response.getAnswers().getWords());
        if (CrackingStatus.IN_PROGRESS.equals(currentStatus.getStatus())) {
            currentStatus.getData().addAll(response.getAnswers().getWords());
            currentStatus.getFinishedWorkers().add(response.getPartNumber());
            log.info("task {} has {} answers, need {} answers", response.getRequestId(),
                    currentStatus.getFinishedWorkers().size(), workersAmount);
            if (currentStatus.getFinishedWorkers().size() == workersAmount) {
                currentStatus.setStatus(CrackingStatus.READY);
                log.info("task {} is ready!", response.getRequestId());
            }
            if (Instant.now().minus(workerTimeoutInMillis, ChronoUnit.MILLIS).isAfter(currentStatus.getStartTime())) {
                currentStatus.setStatus(CrackingStatus.ERROR);
            }
            taskRepository.save(currentStatus);
        }
    }

    public CrackingStatusResponse getStatus(UUID id) {
        var task = taskRepository.findById(id)
                .orElseThrow();
        return taskMapper.taskEntityToStatusResponse(task);
    }

    private void sendRequestsToWorkers(UUID requestId, String hash, Integer maxLength) {
        log.info("Send task {} to {} workers", requestId, workersAmount);
        for (int i = 0; i < workersAmount; i++) {
            CrackHashManagerRequest request = new CrackHashManagerRequest();
            request.setRequestId(requestId.toString());
            CrackHashManagerRequest.Alphabet alph = new CrackHashManagerRequest.Alphabet();
            alph.getSymbols().addAll(Utils.ALPHABET);
            request.setAlphabet(alph);
            request.setHash(hash);
            request.setMaxLength(maxLength);
            request.setPartCount(workersAmount);
            request.setPartNumber(i);
            rabbitTemplate.convertAndSend(MQConstants.MANAGER_REQUEST_QUEUE, request);
        }
        log.info("Task {} was sent successfully", requestId);
        timeoutExecutor.schedule(() -> cancelCracking(requestId), workerTimeoutInMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelCracking(UUID id) {
        var status = taskRepository.findById(id)
                .orElseThrow();
        if (!CrackingStatus.READY.equals(status.getStatus())) {
            status.setStatus(CrackingStatus.ERROR);
            log.info("task {} is dead!", id);
            taskRepository.save(status);
        }
    }
}
