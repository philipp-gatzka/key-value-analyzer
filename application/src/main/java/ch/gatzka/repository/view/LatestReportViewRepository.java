package ch.gatzka.repository.view;

import ch.gatzka.core.repository.Repository;
import ch.gatzka.tables.records.LatestReportViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.LATEST_REPORT_VIEW;

@Service
public class LatestReportViewRepository extends Repository<LatestReportViewRecord> {

    public LatestReportViewRepository(DSLContext dslContext) {
        super(dslContext, LATEST_REPORT_VIEW);
    }

}
