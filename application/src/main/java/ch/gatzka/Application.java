package ch.gatzka;

import ch.gatzka.repository.RoleRepository;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Theme(value = "application", variant = Lumo.DARK)
@EnableScheduling
public class Application implements AppShellConfigurator, ApplicationRunner {

    private final RoleRepository roleRepository;

    public Application(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!roleRepository.existsByName("ADMIN")){
            roleRepository.insert(role -> role.setName("ADMIN"));
        }

        if (!roleRepository.existsByName("USER")){
            roleRepository.insert(role -> role.setName("USER"));
        }
    }

}

