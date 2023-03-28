package ru.nsu.nikolotov.crackhash.worker.service;

import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.paukov.combinatorics3.Generator;
import org.springframework.web.client.RestTemplate;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashManagerRequest;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashWorkerResponse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HashCrackerService {

    private static final String PATH_TO_RESPONSE_MANAGER = "/internal/api/manager/hash/crack/request";

    private final RestTemplate restTemplate;

    @Value("${hashcracker.manager-address}")
    private String managerAddress;

    @Async
    public void crackHash(CrackHashManagerRequest request) {
        List<String> str = findStringsByHash(request);
        sendResponseToManager(str, request.getPartNumber(), request.getRequestId());
    }

    private List<String> findStringsByHash(CrackHashManagerRequest request) {
        int maxLen = request.getMaxLength();
        String hash = request.getHash();
        List<String> alphabet = request.getAlphabet().getSymbols();
        byte[] hashBytes = DatatypeConverter.parseHexBinary(hash);
        List<String> strings = new ArrayList<>();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            for (int i = 0; i <= maxLen; i++) {
                Iterator<List<String>> iter = Generator.permutation(alphabet).withRepetitions(i)
                        .stream()
                        .iterator();
                while(iter.hasNext()) {
                    String str = String.join("", iter.next());
                    byte[] md5 = messageDigest.digest(str.getBytes());
                    if (Arrays.equals(md5, hashBytes)) {
                        strings.add(str);
                    }
                }
            }
            return strings;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponseToManager(List<String> words,  Integer partNumber, String requestId) {
        CrackHashWorkerResponse response = new CrackHashWorkerResponse();
        response.setRequestId(requestId);
        response.setPartNumber(partNumber);
        CrackHashWorkerResponse.Answers answers = new CrackHashWorkerResponse.Answers();
        answers.getWords().addAll(words);
        response.setAnswers(answers);
        restTemplate.patchForObject(managerAddress + PATH_TO_RESPONSE_MANAGER, response, Void.class);
    }
}
