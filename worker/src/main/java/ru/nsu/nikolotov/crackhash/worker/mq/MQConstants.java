package ru.nsu.nikolotov.crackhash.worker.mq;

public class MQConstants {

    public static final String CRACK_HASH_EXCHANGE = "crackhash";

    public static final String MANAGER_REQUEST_QUEUE = CRACK_HASH_EXCHANGE + ".manager-request";

    public static final String WORKER_RESPONSE_QUEUE = CRACK_HASH_EXCHANGE + ".worker-response";

}
