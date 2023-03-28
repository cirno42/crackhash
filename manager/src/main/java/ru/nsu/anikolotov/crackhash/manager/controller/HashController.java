package ru.nsu.anikolotov.crackhash.manager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashManagerResponse;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackingStatusResponse;
import ru.nsu.anikolotov.crackhash.manager.service.ManagerService;

import java.util.UUID;

@RestController
@RequestMapping("/api/hash")
@RequiredArgsConstructor
public class HashController {

    private final ManagerService managerService;

    @PostMapping("/crack")
    public CrackHashManagerResponse requestCrackingHash(@RequestBody CrackHashRequest request) {
        UUID requestId = managerService.crackHash(request);
        return new CrackHashManagerResponse(requestId);
    }

    @GetMapping("/status")
    public CrackingStatusResponse getCrackingStatus(@RequestParam UUID requestId) {
        return managerService.getStatus(requestId);
    }
}
