package ch.gatzka.repository.view;

import ch.gatzka.core.repository.ReadOnlyRepository;
import ch.gatzka.enums.GameMode;
import ch.gatzka.tables.records.KeyReportViewRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.KEY_REPORT_VIEW;

@Service
public class KeyReportViewRepository extends ReadOnlyRepository<KeyReportViewRecord> {

    public KeyReportViewRepository(DSLContext dslContext) {
        super(dslContext, KEY_REPORT_VIEW);
    }

    public Result<KeyReportViewRecord> readByGameMode(GameMode gameMode) {
        return read(KEY_REPORT_VIEW.GAME_MODE.eq(gameMode));
    }

}
