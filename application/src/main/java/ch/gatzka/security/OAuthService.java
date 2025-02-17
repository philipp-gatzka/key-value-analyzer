package ch.gatzka.security;

import ch.gatzka.repository.AccountRepository;
import ch.gatzka.repository.AccountRoleRepository;
import ch.gatzka.repository.RoleRepository;
import ch.gatzka.tables.records.AccountRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;

    private final AccountRoleRepository accountRoleRepository;

    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");

        if (!accountRepository.existsByEmail(email)) {
            log.info("Creating new user account for email: {}", email);
            Integer accountId = accountRepository.insertWithSequence(account -> account.setEmail(email));
            Integer userRoleId = roleRepository.getUserRole().getId();
            accountRoleRepository.insert(accountRole -> accountRole.setAccountId(accountId).setRoleId(userRoleId));
        }

        return new DefaultOAuth2User(getAuthorities(accountRepository.findByEmail(email).orElseThrow()), oauthUser.getAttributes(), "email");
    }

    private List<GrantedAuthority> getAuthorities(AccountRecord account) {
        return accountRoleRepository.readByAccountId(account.getId()).map(accountRole -> roleRepository.findById(accountRole.getRoleId())).stream().filter(Optional::isPresent).map(role -> new SimpleGrantedAuthority("ROLE_" + role.get().getName())).collect(Collectors.toList());
    }
}
