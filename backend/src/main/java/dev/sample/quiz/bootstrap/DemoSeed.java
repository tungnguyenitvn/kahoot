package dev.sample.quiz.bootstrap;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import dev.sample.quiz.catalog.application.QuizCatalog;
import dev.sample.quiz.catalog.domain.Draft;
import dev.sample.quiz.catalog.domain.Question;
import dev.sample.quiz.identity.application.Accounts;
@Component
public class DemoSeed implements ApplicationRunner {
    private final Accounts accounts;private final QuizCatalog catalog;
    @Value("${app.demo.enabled}") boolean enabled;
    @Value("${app.demo.username}") String username;
    @Value("${app.demo.password}") String password;
    public DemoSeed(Accounts accounts,QuizCatalog catalog){this.accounts=accounts;this.catalog=catalog;}
    public void run(ApplicationArguments args){
        if(!enabled)return;
        UUID id=accounts.register(username,"Demo Host",password).id();
        if(catalog.list(id).isEmpty()){
            var draft=new Draft("Java & Angular",List.of(
                new Question("Java: từ khóa nào khai báo record?",List.of("record","struct","data","tuple"),0,15),
                new Question("Angular: primitive nào lưu state phản ứng?",List.of("Promise","signal","Thread","Servlet"),1,15),
                new Question("HTTP status nào thường biểu thị xung đột?",List.of("200","301","409","503"),2,15)));
            var q=catalog.create(id,draft);catalog.publish(id,UUID.fromString(q.id()));
        }
    }
}
