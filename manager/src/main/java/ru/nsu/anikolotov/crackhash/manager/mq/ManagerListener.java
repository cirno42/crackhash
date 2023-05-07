package ru.nsu.anikolotov.crackhash.manager.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashWorkerResponse;
import ru.nsu.anikolotov.crackhash.manager.service.ManagerService;

@Service
@RequiredArgsConstructor
public class ManagerListener {

    private final ManagerService managerService;

    @RabbitListener(queues = MQConstants.WORKER_RESPONSE_QUEUE)
    public void acceptWorkerResponse(CrackHashWorkerResponse response) {
        managerService.updateStatus(response);
    }
}
