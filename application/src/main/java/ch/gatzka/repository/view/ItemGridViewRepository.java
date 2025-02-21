package ch.gatzka.repository.view;

import ch.gatzka.core.repository.ReadOnlyRepository;
import ch.gatzka.tables.records.ItemGridViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;

@Service
public class ItemGridViewRepository extends ReadOnlyRepository<ItemGridViewRecord> {

    public ItemGridViewRepository(DSLContext dslContext) {
        super(dslContext, ITEM_GRID_VIEW);
    }

}
