package com.dts.scheduler.validation;

import com.dts.scheduler.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator ssrfValidator;

    @BeforeEach
    void setUp() {
        ssrfValidator = new SsrfValidator();
    }

    @Test
    @DisplayName("Should permit safe public HTTPS URLs")
    void testPermitSafePublicUrl() {
        assertDoesNotThrow(() -> ssrfValidator.validateSafeUrl("https://httpbin.org/get"));
        assertDoesNotThrow(() -> ssrfValidator.validateSafeUrl("https://example.com/api/v1/webhook"));
    }

    @Test
    @DisplayName("Should reject loopback and localhost addresses (SSRF)")
    void testRejectLocalhost() {
        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                ssrfValidator.validateSafeUrl("http://localhost:8080/internal"));
        assertTrue(ex1.getMessage().contains("prohibited"));

        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                ssrfValidator.validateSafeUrl("http://127.0.0.1:9092"));
        assertTrue(ex2.getMessage().contains("prohibited"));
    }

    @Test
    @DisplayName("Should reject AWS / Cloud Metadata endpoint (169.254.169.254)")
    void testRejectCloudMetadata() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ssrfValidator.validateSafeUrl("http://169.254.169.254/latest/meta-data/"));
        assertTrue(ex.getMessage().contains("prohibited"));
    }

    @Test
    @DisplayName("Should reject non-HTTP/HTTPS protocols such as file:// and ftp://")
    void testRejectInvalidProtocols() {
        assertThrows(BadRequestException.class, () ->
                ssrfValidator.validateSafeUrl("file:///etc/passwd"));
        assertThrows(BadRequestException.class, () ->
                ssrfValidator.validateSafeUrl("ftp://files.internal/secret.key"));
    }
}
