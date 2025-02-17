package ch.gatzka.repository.view;

import ch.gatzka.core.repository.Repository;
import ch.gatzka.tables.records.HighestItemPriceRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Tables.HIGHEST_ITEM_PRICE;

@Service
public class HighestItemPriceRepository extends Repository<HighestItemPriceRecord> {

    public HighestItemPriceRepository(DSLContext dslContext) {
        super(dslContext, HIGHEST_ITEM_PRICE);
    }

    public Optional<HighestItemPriceRecord> findByItemId(int itemId) {
        return find(HIGHEST_ITEM_PRICE.ITEM_ID.eq(itemId));
    }
}
