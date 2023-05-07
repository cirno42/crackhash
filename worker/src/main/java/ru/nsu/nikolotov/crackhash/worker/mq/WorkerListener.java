package ru.nsu.nikolotov.crackhash.worker.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashManagerRequest;
import ru.nsu.nikolotov.crackhash.worker.service.HashCrackerService;

@Service
@RequiredArgsConstructor
public class WorkerListener {

    private final HashCrackerService hashCrackerService;

    @RabbitListener(queues = MQConstants.MANAGER_REQUEST_QUEUE)
    public void requestCrackingHash(CrackHashManagerRequest request) {
        hashCrackerService.crackHash(request);
    }
}
