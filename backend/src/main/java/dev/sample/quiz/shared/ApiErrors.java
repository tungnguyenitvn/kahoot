package dev.sample.quiz.shared;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.DataAccessException;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status).body(Map.of("code",e.code)); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,IllegalArgumentException.class})
    ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST")); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> unavailable(Exception e) { return ResponseEntity.status(503).body(Map.of("code","STORAGE_UNAVAILABLE")); }
}
