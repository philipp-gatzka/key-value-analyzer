package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.KeyRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Sequences.KEY_ID_SEQ;
import static ch.gatzka.Tables.KEY;

@Service
public class KeyRepository extends CrudRepository<KeyRecord> {

    public KeyRepository(DSLContext dslContext) {
        super(dslContext, KEY, KEY_ID_SEQ, KEY.ID);
    }

}
