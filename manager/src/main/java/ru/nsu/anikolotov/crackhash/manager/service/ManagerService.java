package ru.nsu.anikolotov.crackhash.manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashManagerRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashWorkerResponse;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackingStatusResponse;
import ru.nsu.anikolotov.crackhash.manager.enums.CrackingStatus;
import ru.nsu.anikolotov.crackhash.manager.utils.Utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private static final String PATH_TO_CRACK_REQUEST = "/internal/api/worker/hash/crack/task";

    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);
    private final Map<UUID, CrackingStatusResponse> statuses = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    @Value("${hashcracker.worker-address}")
    private String workerAddress;

    @Value("${hashcracker.worker-timeout}")
    private Long workerTimeoutInMillis;

    public UUID crackHash(CrackHashRequest request) {
        UUID requestUuid = UUID.randomUUID();
        CrackingStatusResponse status = new CrackingStatusResponse(CrackingStatus.IN_PROGRESS, null);
        statuses.put(requestUuid, status);
        sendRequestsToWorkers(requestUuid, request.getHash(), request.getMaxLength());

        return requestUuid;
    }

    public void updateStatus(CrackHashWorkerResponse response) {
        UUID requestId = UUID.fromString(response.getRequestId());
        var currentStatus = statuses.get(requestId);
        if (CrackingStatus.IN_PROGRESS.equals(currentStatus.getStatus())) {
            if (currentStatus.getData() == null) {
                currentStatus.setStatus(CrackingStatus.READY);
                currentStatus.setData(response.getAnswers().getWords());
            } else {
                currentStatus.getData().addAll(response.getAnswers().getWords());
            }
        }
    }

    public CrackingStatusResponse getStatus(UUID id) {
        return statuses.get(id);
    }

    private void sendRequestsToWorkers(UUID requestId, String hash, Integer maxLength) {
        CrackHashManagerRequest request = new CrackHashManagerRequest();
        request.setRequestId(requestId.toString());
        CrackHashManagerRequest.Alphabet alph = new CrackHashManagerRequest.Alphabet();
        alph.getSymbols().addAll(Utils.ALPHABET);
        request.setAlphabet(alph);
        request.setHash(hash);
        request.setMaxLength(maxLength);
        request.setPartCount(1);
        request.setPartNumber(0);
        restTemplate.postForObject(workerAddress + PATH_TO_CRACK_REQUEST, request, Void.class);
        timeoutExecutor.schedule(() -> cancelCracking(requestId), workerTimeoutInMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelCracking(UUID id) {
        var status = statuses.get(id);
        if (!CrackingStatus.READY.equals(status.getStatus())) {
            status.setStatus(CrackingStatus.ERROR);
        }
    }
}
