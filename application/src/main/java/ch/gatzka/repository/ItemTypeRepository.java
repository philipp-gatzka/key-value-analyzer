package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.ItemTypeRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.ITEM_TYPE;

@Service
public class ItemTypeRepository extends CrudRepository<ItemTypeRecord> {

    public ItemTypeRepository(DSLContext dslContext) {
        super(dslContext, ITEM_TYPE);
    }

    public Result<ItemTypeRecord> readByItemId(int itemId){
        return read(ITEM_TYPE.ITEM_ID.eq(itemId));
    }

    public void deleteByItemId(int itemId){
        delete(ITEM_TYPE.ITEM_ID.eq(itemId));
    }

}
