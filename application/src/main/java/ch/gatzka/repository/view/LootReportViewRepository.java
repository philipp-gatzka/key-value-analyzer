package ch.gatzka.repository.view;

import ch.gatzka.core.repository.ReadOnlyRepository;
import ch.gatzka.tables.records.LootReportViewRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.LOOT_REPORT_VIEW;

@Service
public class LootReportViewRepository extends ReadOnlyRepository<LootReportViewRecord> {

    public LootReportViewRepository(DSLContext dslContext) {
        super(dslContext, LOOT_REPORT_VIEW);
    }

    public Result<LootReportViewRecord> readByKeyReportId(int keyReportId) {
        return read(LOOT_REPORT_VIEW.KEY_REPORT_ID.eq(keyReportId));
    }

}
