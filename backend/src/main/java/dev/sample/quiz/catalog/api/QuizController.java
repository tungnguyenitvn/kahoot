package dev.sample.quiz.catalog.api;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import dev.sample.quiz.catalog.application.QuizCatalog;
import dev.sample.quiz.catalog.domain.Draft;
import dev.sample.quiz.catalog.domain.Question;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.identity.application.Identities;
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizCatalog catalog; private final Identities identities;
    public QuizController(QuizCatalog catalog, Identities identities) { this.catalog = catalog; this.identities = identities; }
    /** Wire shape of a draft (docs/contracts/rest-api.md). The bounds are the domain constants, so a bad body is INVALID_REQUEST before the domain is built. */
    public record QuestionRequest(@NotBlank @Size(max = Question.MAX_TEXT) String text,
                                  @NotNull @Size(min = Question.OPTIONS, max = Question.OPTIONS) List<@NotBlank @Size(max = Question.MAX_OPTION) String> options,
                                  @NotNull @Min(0) @Max(Question.OPTIONS - 1) Integer correctOption,
                                  @NotNull @Min(Question.MIN_SECONDS) @Max(Question.MAX_SECONDS) Integer seconds) {
        Question toDomain() { return new Question(text, options, correctOption, seconds); }
    }
    public record DraftRequest(@NotBlank @Size(max = Draft.MAX_TITLE) String title,
                               @Valid @NotEmpty @Size(max = Draft.MAX_QUESTIONS) List<@NotNull QuestionRequest> questions) {
        Draft toDomain() { return new Draft(title, questions.stream().map(QuestionRequest::toDomain).toList()); }
    }
    @GetMapping List<Quiz> list(HttpServletRequest r) { return catalog.list(identities.host(r).userId()); }
    @PostMapping Quiz create(HttpServletRequest r, @Valid @RequestBody DraftRequest draft) { return catalog.create(identities.host(r).userId(), draft.toDomain()); }
    @PostMapping("/{id}/publish") Quiz publish(HttpServletRequest r, @PathVariable UUID id) { return catalog.publish(identities.host(r).userId(), id); }
}
