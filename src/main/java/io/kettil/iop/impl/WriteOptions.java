package io.kettil.iop.impl;

import io.kettil.iop.Message;
import lombok.Data;

@Data
public class WriteOptions {
    private int userId;
    private int deviceId;
    private String messageId;
    private Message messageBody;
}
