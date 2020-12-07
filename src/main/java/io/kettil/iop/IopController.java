package io.kettil.iop;

import io.kettil.iop.impl.IopService;
import io.kettil.iop.impl.ReadOptions;
import io.kettil.iop.impl.WriteConfirmation;
import io.kettil.iop.impl.WriteOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
public class IopController {
    @Autowired
    IopService iopService;


    @Data
    public static class ReadRequest {
        private int userId;
        private int deviceId;
    }

    // curl http://localhost:8080/read -X POST -H 'Content-Type: application/json' -d '{"userId": 2, "deviceId": 5}'

    @PostMapping(
        value = "/read",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Message> read(@RequestBody ReadRequest req) {
        ReadOptions readOptions = new ReadOptions();
        readOptions.setUserId(req.getUserId());
        readOptions.setDeviceId(req.getDeviceId());

        return iopService.read(readOptions);
    }

    @Data
    public static class WriteRequest {
        private int userId;
        private int deviceId;
        private String messageId;
        private Message messageBody;
    }

    @Data
    public static class WriteResponse {
        private String messageId;
        private Instant timestamp;
    }

    // curl http://localhost:8080/write -X POST -H 'Content-Type: application/json' -d '{"userId": 2, "deviceId": 5, "messageId": 123, "messageBody": {"userId": 2, "deviceId": 5, "messageId": 123, "messageType": "info", "messageBody": "random"}}'

    // curl http://localhost:8080/write -X POST -H 'Content-Type: application/json' -d '{"userId": 2, "deviceId": 5, "messageId": 123, "messageBody": {"userId": 2, "messageId": 123, "messageType": "info", "messageBody": "random"}}'

    @PostMapping(
        value = "/write",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<WriteResponse> write(@RequestBody WriteRequest req) {
        WriteOptions writeOptions = new WriteOptions();
        writeOptions.setUserId(req.getUserId());
        writeOptions.setDeviceId(req.getDeviceId());
        writeOptions.setMessageId(req.getMessageId());
        writeOptions.setMessageBody(req.getMessageBody());

        return iopService
            .write(writeOptions)
            .map(i -> {
                WriteResponse writeResponse = new WriteResponse();
                writeResponse.setMessageId(req.getMessageId());
                writeResponse.setTimestamp(Instant.now());
                return writeResponse;
            });


    }
}
