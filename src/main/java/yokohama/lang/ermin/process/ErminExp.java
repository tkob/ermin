package yokohama.lang.ermin.process;

public interface ErminExp {
    <R> R accept(ErminExpVisitor<R> visitor);
}
