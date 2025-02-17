package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.AccountRoleRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.ACCOUNT_ROLE;

@Service
public class AccountRoleRepository extends CrudRepository<AccountRoleRecord> {

    public AccountRoleRepository(DSLContext dslContext) {
        super(dslContext, ACCOUNT_ROLE);
    }

    public Result<AccountRoleRecord> readByAccountId(int accountId){
        return read(ACCOUNT_ROLE.ACCOUNT_ID.eq(accountId));
    }


}
