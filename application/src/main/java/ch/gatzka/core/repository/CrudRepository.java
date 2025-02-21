package ch.gatzka.core.repository;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public abstract class CrudRepository<R extends UpdatableRecord<R>> extends ReadOnlyRepository<R> {

    @Nullable
    private final Sequence<Integer> sequence;

    private final TableField<R, Integer> idField;

    public CrudRepository(DSLContext dslContext, Table<R> table) {
        super(dslContext, table);
        this.sequence = null;
        this.idField = null;
    }

    public CrudRepository(DSLContext dslContext, Table<R> table, @Nullable Sequence<Integer> sequence, TableField<R, Integer> idField) {
        super(dslContext, table);
        this.sequence = sequence;
        this.idField = idField;
    }

    public Integer insertWithSequence(Function<R, R> mapping) {
        if (sequence == null || idField == null) {
            throw new IllegalStateException("Table does not have a sequenced id field");
        }

        return dslContext.transactionResult(configuration -> {
            DSLContext dslContext = configuration.dsl();
            Integer id = dslContext.nextval(sequence);

            R newRecord = dslContext.newRecord(table);
            newRecord.set(idField, id);

            mapping.apply(newRecord).insert();

            return id;
        });
    }

    public void insert(Function<R, R> mapping) {
        mapping.apply(dslContext.newRecord(table)).insert();
    }

    public void update(Function<R, R> mapping, Condition... condition) {
        dslContext.fetch(table, condition).forEach(record -> mapping.apply(record).update());
    }

    public void delete(Condition... condition) {
        dslContext.deleteFrom(table).where(condition).execute();
    }

    public Optional<R> findById(Integer id) {
        if (idField == null) {
            throw new IllegalStateException("Table does not have a id field");
        }
        return dslContext.fetchOptional(table, idField.eq(id));
    }

}
