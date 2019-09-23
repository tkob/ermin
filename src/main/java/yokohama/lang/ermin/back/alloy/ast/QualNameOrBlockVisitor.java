package yokohama.lang.ermin.back.alloy.ast;

public interface QualNameOrBlockVisitor<R> {
    R visitQualName(QualName qualName);

    R visitBlock(Block block);
}
