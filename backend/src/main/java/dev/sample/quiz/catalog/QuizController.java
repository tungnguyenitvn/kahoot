package dev.sample.quiz.catalog;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import dev.sample.quiz.identity.Identity;
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizCatalog catalog;
    public QuizController(QuizCatalog catalog) {this.catalog=catalog;}
    @GetMapping List<QuizCatalog.Quiz> list(HttpServletRequest r) {return catalog.list(Identity.current(r).userId());}
    @PostMapping QuizCatalog.Quiz create(HttpServletRequest r,@Valid @RequestBody QuizCatalog.Draft draft) {return catalog.create(Identity.current(r).userId(),draft);}
    @PostMapping("/{id}/publish") QuizCatalog.Quiz publish(HttpServletRequest r,@PathVariable UUID id) {return catalog.publish(Identity.current(r).userId(),id);}
}
