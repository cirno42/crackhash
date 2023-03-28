package ru.nsu.anikolotov.crackhash.manager.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CrackHashRequest {

    private String hash;
    private Integer maxLength;
}
