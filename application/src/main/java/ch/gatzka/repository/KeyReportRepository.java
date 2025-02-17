package ch.gatzka.repository;

import ch.gatzka.core.repository.CrudRepository;
import ch.gatzka.tables.records.KeyReportRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Sequences.KEY_REPORT_ID_SEQ;
import static ch.gatzka.Tables.KEY_REPORT;

@Service
public class KeyReportRepository extends CrudRepository<KeyReportRecord> {

    public KeyReportRepository(DSLContext dslContext) {
        super(dslContext, KEY_REPORT, KEY_REPORT_ID_SEQ, KEY_REPORT.ID);
    }

}
