package io.kettil.iop.impl;

import lombok.Data;

import java.time.Instant;

@Data
public class WriteConfirmation {
    private String messageId;
    private Instant time;
}
