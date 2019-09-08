package yokohama.lang.ermin.process;

public interface ErminArgument {
    <R> R accept(ErminArgumentVisitor<R> visitor);
}
