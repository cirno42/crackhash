package ru.nsu.anikolotov.crackhash.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class CrackHashManagerResponse {

    private UUID requestId;
}
