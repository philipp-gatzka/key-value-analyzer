package ch.gatzka.repository.view;

import ch.gatzka.core.repository.ReadOnlyRepository;
import ch.gatzka.tables.records.ItemPriceViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Tables.ITEM_PRICE_VIEW;

@Service
public class ItemPriceViewRepository extends ReadOnlyRepository<ItemPriceViewRecord> {

    public ItemPriceViewRepository(DSLContext dslContext) {
        super(dslContext, ITEM_PRICE_VIEW);
    }

    public Optional<ItemPriceViewRecord> findByItemId(int itemId) {
        return find(ITEM_PRICE_VIEW.ITEM_ID.eq(itemId));
    }

}
