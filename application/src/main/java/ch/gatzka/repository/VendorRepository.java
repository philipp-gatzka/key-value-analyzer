package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.VendorRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static ch.gatzka.Sequences.VENDOR_ID_SEQ;
import static ch.gatzka.Tables.VENDOR;

@Service
public class VendorRepository extends CrudRepository<VendorRecord> {

    public VendorRepository(DSLContext dslContext) {
        super(dslContext, VENDOR, VENDOR_ID_SEQ, VENDOR.ID);
    }

    public Optional<VendorRecord> findByName(String name) {
        return find(VENDOR.NAME.eq(name));
    }
}
