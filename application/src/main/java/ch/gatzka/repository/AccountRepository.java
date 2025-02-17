package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.AccountRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.ACCOUNT_ID_SEQ;
import static ch.gatzka.Tables.ACCOUNT;

@Service
public class AccountRepository extends CrudRepository<AccountRecord> {

    public AccountRepository(DSLContext dslContext) {
        super(dslContext, ACCOUNT, ACCOUNT_ID_SEQ, ACCOUNT.ID);
    }

    public Optional<AccountRecord> findByEmail(String username) {
        return find(ACCOUNT.EMAIL.eq(username));
    }

    public boolean existsByEmail(String username) {
        return exists(ACCOUNT.EMAIL.eq(username));
    }
}
