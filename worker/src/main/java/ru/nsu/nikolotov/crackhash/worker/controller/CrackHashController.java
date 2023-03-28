package ru.nsu.nikolotov.crackhash.worker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashManagerRequest;
import ru.nsu.nikolotov.crackhash.worker.service.HashCrackerService;

@RequestMapping("/internal/api/worker")
@RequiredArgsConstructor
@RestController
public class CrackHashController {

    private final HashCrackerService hashCrackerService;

    @PostMapping("/hash/crack/task")
    public void requestCrackingHash(@RequestBody CrackHashManagerRequest request) {
        hashCrackerService.crackHash(request);
    }
}
