package ch.gatzka.repository.view;

import ch.gatzka.core.repository.Repository;
import ch.gatzka.tables.records.LootReportRecord;
import ch.gatzka.tables.records.LootReportViewRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.LOOT_REPORT;
import static ch.gatzka.Tables.LOOT_REPORT_VIEW;

@Service
public class LootReportViewRepository extends Repository<LootReportViewRecord> {

    public LootReportViewRepository(DSLContext dslContext) {
        super(dslContext, LOOT_REPORT_VIEW);
    }

    public Result<LootReportViewRecord> findByKeyReportId(int keyReportId) {
        return read(LOOT_REPORT_VIEW.KEY_REPORT_ID.eq(keyReportId));

    }
}
