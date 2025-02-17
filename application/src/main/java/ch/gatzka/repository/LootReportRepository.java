package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.KeyReportRecord;
import ch.gatzka.tables.records.LootReportRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Sequences.LOOT_REPORT_ID_SEQ;
import static ch.gatzka.Tables.LOOT_REPORT;

@Service
public class LootReportRepository extends CrudRepository<LootReportRecord> {

    public LootReportRepository(DSLContext dslContext) {
        super(dslContext, LOOT_REPORT, LOOT_REPORT_ID_SEQ, LOOT_REPORT.ID);
    }

}
