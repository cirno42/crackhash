package ru.nsu.nikolotov.crackhash.worker.service;

import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
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
@RequiredArgsConstructor
public class HashCrackerService {

    private final RabbitTemplate rabbitTemplate;

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
                        .skip(getFirstSkipCount(request, i))
                        .limit(getLastSkipCount(request, i))
                        .iterator();
                while (iter.hasNext()) {
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

    private void sendResponseToManager(List<String> words, Integer partNumber, String requestId) {
        CrackHashWorkerResponse response = new CrackHashWorkerResponse();
        response.setRequestId(requestId);
        response.setPartNumber(partNumber);
        CrackHashWorkerResponse.Answers answers = new CrackHashWorkerResponse.Answers();
        answers.getWords().addAll(words);
        response.setAnswers(answers);
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
        return skip.longValue();
    }

    private long getLastSkipCount(CrackHashManagerRequest request, int curLength) {
        BigInteger totalWords =
                BigInteger.valueOf(request.getAlphabet().getSymbols().size())
                        .pow(curLength);
        BigInteger wordsInPart = totalWords
                .divide(BigInteger.valueOf(request.getPartCount()));
        BigInteger skip = BigInteger.valueOf(request.getPartCount())
                .subtract(BigInteger.valueOf(request.getPartNumber() + 1))
                .multiply(wordsInPart);
        return skip.longValue();
    }
}
