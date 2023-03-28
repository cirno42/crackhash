package ru.nsu.anikolotov.crackhash.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.anikolotov.crackhash.manager.enums.CrackingStatus;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class CrackingStatusResponse {

    private CrackingStatus status;
    private List<String> data;
}
