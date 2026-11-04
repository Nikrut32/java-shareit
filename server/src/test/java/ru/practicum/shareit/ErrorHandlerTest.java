package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.shareit.exception.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    @InjectMocks
    private ErrorHandler errorHandler;

    @Mock
    private DataProcessingException dataProcessingException;
    @Mock
    private ValidationNotObjectException validationNotObjectException;
    @Mock
    private ValidationException validationException;
    @Mock
    private ConflictObjectException conflictObjectException;
    @Mock
    private AccessException accessException;

    @Test
    void handleDataProcessingNotFoundTest() {
        when(dataProcessingException.getMessage()).thenReturn("нельзя бронировать свою вещь");

        ErrorResponse r = errorHandler.handleDataProcessing(dataProcessingException);

        assertEquals("Ошибка обработки данных", r.getError());
        assertEquals("нельзя бронировать свою вещь", r.getDescription());
        verify(dataProcessingException, times(1)).getMessage();
        verifyNoMoreInteractions(dataProcessingException);
    }

    @Test
    void handleValidationNotObjectNotFoundTest() {
        when(validationNotObjectException.getMessage()).thenReturn("вещь не найдена");

        ErrorResponse r = errorHandler.handleValidationNotObject(validationNotObjectException);

        assertEquals("Искомый объект не найден", r.getError());
        assertEquals("вещь не найдена", r.getDescription());
        verify(validationNotObjectException).getMessage();
        verifyNoMoreInteractions(validationNotObjectException);
    }

    @Test
    void handleValidationBadRequestTest() {
        when(validationException.getMessage()).thenReturn("дата начала в прошлом");

        ErrorResponse r = errorHandler.handleValidation(validationException);

        assertEquals("Ошибка валидации", r.getError());
        assertEquals("дата начала в прошлом", r.getDescription());
        verify(validationException).getMessage();
        verifyNoMoreInteractions(validationException);
    }

    @Test
    void handleConflictObjectConflictTest() {
        when(conflictObjectException.getMessage()).thenReturn("email уже занят");

        ErrorResponse r = errorHandler.handleConflictObject(conflictObjectException);

        assertEquals("Конфликт объектов", r.getError());
        assertEquals("email уже занят", r.getDescription());
        verify(conflictObjectException).getMessage();
        verifyNoMoreInteractions(conflictObjectException);
    }

    @Test
    void handleAccessUnauthorizedTest() {
        when(accessException.getMessage()).thenReturn("нет прав на редактирование");

        ErrorResponse r = errorHandler.handleAccess(accessException);

        assertEquals("Ошибка доступа", r.getError());
        assertEquals("нет прав на редактирование", r.getDescription());
        verify(accessException).getMessage();
        verifyNoMoreInteractions(accessException);
    }

    @Test
    void responseStatusesCorrectTest() throws Exception {
        assertStatus("handleDataProcessing", DataProcessingException.class, HttpStatus.NOT_FOUND);
        assertStatus("handleValidationNotObject", ValidationNotObjectException.class, HttpStatus.NOT_FOUND);
        assertStatus("handleValidation", ValidationException.class, HttpStatus.BAD_REQUEST);
        assertStatus("handleConflictObject", ConflictObjectException.class, HttpStatus.CONFLICT);
        assertStatus("handleAccess", AccessException.class, HttpStatus.UNAUTHORIZED);
    }

    private void assertStatus(String method, Class<?> arg, HttpStatus expected) throws Exception {
        Method m = ErrorHandler.class.getMethod(method, arg);
        ResponseStatus a = m.getAnnotation(ResponseStatus.class);
        assertNotNull(a, "Нет @ResponseStatus у " + method);
        assertEquals(expected, a.value(), "Неверный статус у " + method);
    }
}