package com.vinaacademy.platform.client.decoder;

import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.common.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        String errorMessage = extractErrorMessage(response);

        log.error("Feign client error: {} - {} - {}", methodKey, status, errorMessage);

        return switch (status) {
            case BAD_REQUEST -> BadRequestException.message(errorMessage);
            case NOT_FOUND -> new ResourceNotFoundException(errorMessage);
            case UNAUTHORIZED -> new RuntimeException("Unauthorized: " + errorMessage);
            case FORBIDDEN -> new RuntimeException("Forbidden: " + errorMessage);
            default -> new RuntimeException("Service call failed: " + errorMessage);
        };
    }

    private String extractErrorMessage(Response response) {
        try {
            if (response.body() != null) {
                byte[] bodyBytes = response.body().asInputStream().readAllBytes();
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                
                // Parse JSON response to extract message if needed
                // For now, return the full body
                return body.isEmpty() ? "Unknown error" : body;
            }
        } catch (IOException e) {
            log.error("Error reading response body", e);
        }
        return "Unknown error occurred";
    }
}
