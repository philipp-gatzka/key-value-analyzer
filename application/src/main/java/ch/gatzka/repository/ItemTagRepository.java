package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.ItemTagRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.ITEM_TAG;

@Service
public class ItemTagRepository extends CrudRepository<ItemTagRecord> {

    public ItemTagRepository(DSLContext dslContext) {
        super(dslContext, ITEM_TAG);
    }

    public void deleteByItemId(int itemId){
        delete(ITEM_TAG.ITEM_ID.eq(itemId));
    }

}
