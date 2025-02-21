package ch.gatzka.repository.view;

import ch.gatzka.core.repository.ReadOnlyRepository;
import ch.gatzka.tables.records.TagViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.TAG_VIEW;

@Service
public class TagViewRepository extends ReadOnlyRepository<TagViewRecord> {

    public TagViewRepository(DSLContext dslContext) {
        super(dslContext, TAG_VIEW);
    }

}
