package ch.gatzka.security;

import ch.gatzka.repository.AccountRepository;
import ch.gatzka.tables.records.AccountRecord;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticatedAccount {

    private final AccountRepository accountRepository;

    private final AuthenticationContext authenticationContext;

    public Optional<UserInfo> get() {
        return authenticationContext.getAuthenticatedUser(DefaultOAuth2User.class)
                .flatMap(userDetails -> {
                    Optional<AccountRecord> optionalAccount = accountRepository.findByEmail(userDetails.getName());
                    return optionalAccount.map(account -> new UserInfo(userDetails.getAttributes(), account));
                });
    }

    public void logout() {
        authenticationContext.logout();
    }

    public record UserInfo(Map<String, Object> attributes, AccountRecord account) {

    }

}
