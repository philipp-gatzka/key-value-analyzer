package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.RoleRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.ROLE_ID_SEQ;
import static ch.gatzka.Tables.ROLE;

@Service
public class RoleRepository extends CrudRepository<RoleRecord> {

    public RoleRepository(DSLContext dslContext) {
        super(dslContext, ROLE, ROLE_ID_SEQ, ROLE.ID);
    }

    public Optional<RoleRecord> findById(int id) {
        return find(ROLE.ID.eq(id));
    }

    public boolean existsByName(String name) {
        return exists(ROLE.NAME.eq(name));
    }

    public Optional<RoleRecord> findByName(String name) {
        return find(ROLE.NAME.eq(name));
    }

    public RoleRecord getAdminRole() {
        return findByName("ADMIN").orElseThrow(() -> new RuntimeException("Admin role not found"));
    }

    public RoleRecord getUserRole() {
        return findByName("USER").orElseThrow(() -> new RuntimeException("User role not found"));
    }
}
