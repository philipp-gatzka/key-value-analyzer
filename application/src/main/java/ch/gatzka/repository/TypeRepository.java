package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.TypeRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.TYPE_ID_SEQ;
import static ch.gatzka.Tables.TYPE;

@Service
public class TypeRepository extends CrudRepository<TypeRecord> {

    public TypeRepository(DSLContext dslContext) {
        super(dslContext, TYPE, TYPE_ID_SEQ, TYPE.ID);
    }

    public Optional<TypeRecord> findByName(String name) {
        return find(TYPE.NAME.eq(name));
    }

    public Optional<TypeRecord> findById(int id) {
        return find(TYPE.ID.eq(id));
    }
}
