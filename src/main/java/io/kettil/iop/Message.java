package io.kettil.iop;

import lombok.Data;

@Data
public class Message {
    int userId;
    Integer deviceId;
    String messageId;
    Object messageBody;
    String MessageType;
}
