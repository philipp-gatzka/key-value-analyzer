package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.TagRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.TAG_ID_SEQ;
import static ch.gatzka.Tables.TAG;

@Service
public class TagRepository extends CrudRepository<TagRecord> {

    public TagRepository(DSLContext dslContext) {
        super(dslContext, TAG, TAG_ID_SEQ, TAG.ID);
    }

    public Optional<TagRecord> findByName(String name) {
        return find(TAG.NAME.eq(name));
    }
}
