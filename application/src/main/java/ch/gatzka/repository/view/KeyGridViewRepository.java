package ch.gatzka.repository.view;

import ch.gatzka.core.repository.Repository;
import ch.gatzka.tables.records.KeyGridViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.KEY_GRID_VIEW;

@Service
public class KeyGridViewRepository extends Repository<KeyGridViewRecord> {

    public KeyGridViewRepository(DSLContext dslContext) {
        super(dslContext, KEY_GRID_VIEW);
    }

}
