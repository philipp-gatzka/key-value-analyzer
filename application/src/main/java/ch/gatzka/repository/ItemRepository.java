package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.ItemRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.ITEM_ID_SEQ;
import static ch.gatzka.Tables.ITEM;

@Service
public class ItemRepository extends CrudRepository<ItemRecord> {

    public ItemRepository(DSLContext dslContext) {
        super(dslContext, ITEM, ITEM_ID_SEQ, ITEM.ID);
    }

    public Optional<ItemRecord> findByTarkovId(String tarkovMarketId){
        return find(ITEM.TARKOV_ID.eq(tarkovMarketId));
    }

}
