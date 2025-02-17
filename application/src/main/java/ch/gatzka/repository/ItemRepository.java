package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.ItemRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

import static ch.gatzka.Sequences.ITEM_ID_SEQ;
import static ch.gatzka.Tables.ITEM;

@Service
public class ItemRepository extends CrudRepository<ItemRecord> {

    public ItemRepository(DSLContext dslContext) {
        super(dslContext, ITEM, ITEM_ID_SEQ, ITEM.ID);
    }

    public boolean existsByTarkovId(String tarkovId) {
        return exists(ITEM.TARKOV_ID.eq(tarkovId));
    }

    public void updateByTarkovId(Function<ItemRecord, ItemRecord> mapping, String tarkovId) {
        update(mapping, ITEM.TARKOV_ID.eq(tarkovId));
    }

    public Optional<ItemRecord> findById(int id){
        return find(ITEM.ID.eq(id));
    }

}
