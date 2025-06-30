package com.gayale.transport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTicketException extends RuntimeException {
    private final String existingTicketId;
    private final String checksum;

    public DuplicateTicketException(String message, String existingTicketId, String checksum) {
        super(message);
        this.existingTicketId = existingTicketId;
        this.checksum = checksum;
    }

    public String getExistingTicketId() { return existingTicketId; }
    public String getChecksum() { return checksum; }
}
