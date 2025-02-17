package ch.gatzka.core.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;

import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
public abstract class Repository<R extends TableRecord<R>> {

    protected final DSLContext dslContext;

    protected final Table<R> table;

    public Result<R> readAll() {
        return dslContext.selectFrom(table).fetch();
    }

    public Optional<R> find(Condition... condition) {
        return dslContext.fetchOptional(table, condition);
    }

    public Result<R> read(Condition... condition) {
        return dslContext.fetch(table, condition);
    }

    public boolean exists(Condition... condition) {
        return dslContext.fetchExists(table, condition);
    }


}
