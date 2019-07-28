package yokohama.lang.ermin.type;

public interface ErminType {
    <R> R accept(ErminTypeVisitor<R> visitor);

}
