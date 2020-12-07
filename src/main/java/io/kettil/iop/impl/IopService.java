package io.kettil.iop.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kettil.iop.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class IopService {
//    @Autowired
//    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired
    TopicExchange exchange;

    @Autowired
    ObjectMapper mapper;

    private String routingKey(int userId) {
        return routingKey(userId, null);
    }

    private String routingKey(int userId, Integer deviceId) {
        return String.format(
            "user.%s.device.%s",
            userId <= 0 ? "all" : userId,
            deviceId == null || deviceId <= 0 ? "all" : deviceId
        );
    }

    public Flux<Message> read(ReadOptions value) {
        DirectMessageListenerContainer listener = new DirectMessageListenerContainer(connectionFactory);

        return Flux.<Message>create(
            emitter -> {
                String queueName = routingKey(value.getUserId(), value.getDeviceId());

                Map<String, Object> args = new HashMap<>();

                args.put("x-max-length", 100);
                args.put("x-overflow", "reject-publish");
                args.put("x-expires", Duration.ofSeconds(60).toMillis()); //queue TTL
                args.put("x-message-ttl", Duration.ofSeconds(60).toMillis()); //message TTL

                Queue queue = new Queue(queueName, true, false, true, args);
                queueName = amqpAdmin.declareQueue(queue);

                String allDevicesRoutingKey = routingKey(value.getUserId());
                amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(allDevicesRoutingKey));

                String deviceRoutingKey = routingKey(value.getUserId(), value.getDeviceId());
                amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(deviceRoutingKey));

                listener.addQueues(queue);

                listener.setMessagesPerAck(1);

                listener.setMessageListener(message -> {
                    try {
                        Message m = mapper.readValue(message.getBody(), Message.class);
                        emitter.next(m);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                listener.start();

            }, FluxSink.OverflowStrategy.LATEST)
            .doFinally(signalType -> {
                listener.destroy();
            });
    }

    public Mono<WriteConfirmation> write(WriteOptions value) {
        return Mono.create(sink -> {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
            messageProperties.setMessageId(value.getMessageId());

            byte[] valueAsBytes;

            try {
                valueAsBytes = mapper.writeValueAsBytes(value.getMessageBody());
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                sink.error(e);
                return;
            }

            rabbitTemplate.send(
                exchange.getName(),
                routingKey(value.getUserId(), value.getDeviceId()),
                new org.springframework.amqp.core.Message(valueAsBytes, messageProperties)
            );

            WriteConfirmation ret = new WriteConfirmation();
            ret.setMessageId(value.getMessageId());
            ret.setTime(Instant.now());

            sink.success(ret);
        });
    }
}
