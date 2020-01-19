package yokohama.lang.ermin.process;

public interface ErminBoolExp {
    <R> R accept(ErminBoolExpVisitor<R> visitor);
}
