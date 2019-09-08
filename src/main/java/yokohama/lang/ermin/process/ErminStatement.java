package yokohama.lang.ermin.process;

public interface ErminStatement {
    <R> R accept(ErminStatementVisitor<R> visitor);
}
