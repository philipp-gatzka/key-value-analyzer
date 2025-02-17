package ch.gatzka.repository.view;

import ch.gatzka.core.repository.Repository;
import ch.gatzka.tables.records.KeyReportViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.KEY_REPORT_VIEW;

@Service
public class KeyReportViewRepository extends Repository<KeyReportViewRecord> {

    public KeyReportViewRepository(DSLContext dslContext) {
        super(dslContext, KEY_REPORT_VIEW);
    }

}
