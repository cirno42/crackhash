package ru.nsu.nikolotov.crackhash.worker.service;

import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashManagerRequest;
import ru.nsu.nikolotov.crackhash.worker.dto.CrackHashWorkerResponse;
import ru.nsu.nikolotov.crackhash.worker.mq.MQConstants;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HashCrackerService {

    private final RabbitTemplate rabbitTemplate;

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
            for (int i = 1; i <= maxLen; i++) {
                Iterator<List<String>> iter = Generator.permutation(alphabet).withRepetitions(i)
                        .stream()
                        .skip(getFirstSkipCount(request, i))
                        .limit(getLastLimitCount(request, i))
                        .iterator();
                while (iter.hasNext()) {
                    String str = String.join("", iter.next());
                    log.debug("String: {} Part: {}", str, request.getPartNumber());
                    byte[] md5 = messageDigest.digest(str.getBytes());
                    if (Arrays.equals(md5, hashBytes)) {
                        strings.add(str);
                    }
                }
                log.info("Found strings: request: {} length: {} part: {} strings: {}", request.getRequestId(), i, request.getPartNumber(), strings);
            }
            return strings;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponseToManager(List<String> words, Integer partNumber, String requestId) {
        CrackHashWorkerResponse response = new CrackHashWorkerResponse();
        response.setRequestId(requestId);
        response.setPartNumber(partNumber);
        CrackHashWorkerResponse.Answers answers = new CrackHashWorkerResponse.Answers();
        answers.getWords().addAll(words);
        response.setAnswers(answers);
        log.info("Send response to manager for request: {} part: {} result: {}", requestId, partNumber, words);
        rabbitTemplate.convertAndSend(MQConstants.WORKER_RESPONSE_QUEUE, response);
    }

    private long getFirstSkipCount(CrackHashManagerRequest request, int curLength) {
        BigInteger totalWords =
                BigInteger.valueOf(request.getAlphabet().getSymbols().size())
                        .pow(curLength);
        BigInteger wordsInPart = totalWords
                .divide(BigInteger.valueOf(request.getPartCount()));
        BigInteger mod = totalWords
                .mod(BigInteger.valueOf(request.getPartCount()));
        BigInteger addMod = mod.min(BigInteger.valueOf(request.getPartNumber()));
        BigInteger skip = wordsInPart
                .multiply(BigInteger.valueOf(request.getPartNumber()))
                .add(addMod);
        log.info("Skip First results: request id: {} " +
                "\n    part: {}" +
                "\n    length: {}" +
                "\n    total words : {}," +
                "\n    words in part : {}," +
                "\n    mod: {}," +
                "\n    add mod: {}," +
                "\n    skip: {}," +
                "\n    skip value: {}", request.getRequestId(),
                request.getPartNumber(), curLength,
                totalWords, wordsInPart, mod, addMod,
                skip, skip.longValue());
        return skip.longValue();
    }

    private long getLastLimitCount(CrackHashManagerRequest request, int curLength) {
        BigInteger totalWords =
                BigInteger.valueOf(request.getAlphabet().getSymbols().size())
                        .pow(curLength);
        BigInteger wordsInPart = totalWords
                .divide(BigInteger.valueOf(request.getPartCount()));
        BigInteger mod = totalWords
                .mod(BigInteger.valueOf(request.getPartCount()));
        BigInteger limit = BigInteger.valueOf(wordsInPart.longValue());
        if (mod.compareTo(BigInteger.valueOf(request.getPartNumber() + 1)) > 0) {
            limit = wordsInPart.add(BigInteger.ONE);
        }
        log.info("Skip Last results: request id: {} " +
                        "\n    part: {}" +
                        "\n    length: {}" +
                        "\n    total words : {}," +
                        "\n    words in part : {}," +
                        "\n    mod: {}," +
                        "\n    limit: {}," +
                        "\n    limit value: {}", request.getRequestId(),
                request.getPartNumber(), curLength,
                totalWords, wordsInPart, mod,
                limit, limit.longValue());
        return limit.longValue();
    }
}
