package ch.gatzka.security;

import ch.gatzka.repository.AccountRepository;
import ch.gatzka.repository.AccountRoleRepository;
import ch.gatzka.repository.RoleRepository;
import ch.gatzka.tables.records.AccountRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    private final AccountRoleRepository accountRoleRepository;

    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AccountRecord account = accountRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("No account present with username: " + username));
        return new User(account.getEmail(), null, getAuthorities(account));
    }

    private List<GrantedAuthority> getAuthorities(AccountRecord account) {
        return accountRoleRepository.readByAccountId(account.getId()).map(accountRole -> roleRepository.findById(accountRole.getRoleId())).stream().filter(Optional::isPresent).map(role -> new SimpleGrantedAuthority("ROLE_" + role.get().getName())).collect(Collectors.toList());
    }

}
