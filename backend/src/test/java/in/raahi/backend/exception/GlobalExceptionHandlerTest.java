package in.raahi.backend.exception;

import in.raahi.backend.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleDataIntegrityViolationReturnsConflict409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key value violates unique constraint users_phone_key");
        ResponseEntity<ApiResponse<Object>> res = handler.handleDataIntegrity(ex);

        assertNotNull(res);
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().success);
        assertEquals("DATA_CONFLICT", res.getBody().error.code);
        assertEquals("The request could not be completed due to conflicting records. Please retry.", res.getBody().error.message);
    }

    @Test
    void testHandleDataAccessResourceFailureReturnsServiceUnavailable503() {
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Connection to PostgreSQL refused");
        ResponseEntity<ApiResponse<Object>> res = handler.handleDatabaseUnavailable(ex);

        assertNotNull(res);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().success);
        assertEquals("DATABASE_UNAVAILABLE", res.getBody().error.code);
        assertEquals("Database service is temporarily unavailable. Please try again later.", res.getBody().error.message);
    }

    @Test
    void testHandleCannotCreateTransactionReturnsServiceUnavailable503() {
        CannotCreateTransactionException ex = new CannotCreateTransactionException("Could not open JDBC Connection for transaction");
        ResponseEntity<ApiResponse<Object>> res = handler.handleDatabaseUnavailable(ex);

        assertNotNull(res);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("DATABASE_UNAVAILABLE", res.getBody().error.code);
    }

    @Test
    void testHandleGeneralDataAccessExceptionReturnsInternalServerError500() {
        CannotAcquireLockException ex = new CannotAcquireLockException("Lock wait timeout");
        ResponseEntity<ApiResponse<Object>> res = handler.handleDatabase(ex);

        assertNotNull(res);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().success);
        assertEquals("DATABASE_ERROR", res.getBody().error.code);
        assertEquals("Database service error. Please try again later.", res.getBody().error.message);
    }
}
