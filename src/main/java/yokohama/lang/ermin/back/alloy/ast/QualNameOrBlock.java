package yokohama.lang.ermin.back.alloy.ast;

public interface QualNameOrBlock extends ToString {
    <R> R accept(QualNameOrBlockVisitor<R> visitor);
}
