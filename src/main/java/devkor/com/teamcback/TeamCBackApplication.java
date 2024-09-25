package devkor.com.teamcback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(servers = {@Server(url = "https://be.kodaero.site")})
@EnableScheduling
@SpringBootApplication
public class TeamCBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamCBackApplication.class, args);
    }

}
