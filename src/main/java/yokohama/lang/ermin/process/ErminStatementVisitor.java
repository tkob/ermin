package yokohama.lang.ermin.process;

public interface ErminStatementVisitor<R> {
    R visitInsertStatement(ErminInsertStatement insertStatement);

    R visitDeleteStatement(ErminDeleteStatement deleteStatement);

    R visitUpdateStatement(ErminUpdateStatement updateStatement);
}
