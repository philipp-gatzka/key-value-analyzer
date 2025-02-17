package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.ItemSaleRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Tables.ITEM_SALE;

@Service
public class ItemSaleRepository extends CrudRepository<ItemSaleRecord> {

    public ItemSaleRepository(DSLContext dslContext) {
        super(dslContext, ITEM_SALE);
    }

    public Optional<ItemSaleRecord> findByItemIdAndVendorId(int itemId, int vendorId) {
        return find(ITEM_SALE.ITEM_ID.eq(itemId), ITEM_SALE.VENDOR_ID.eq(vendorId));
    }

}
