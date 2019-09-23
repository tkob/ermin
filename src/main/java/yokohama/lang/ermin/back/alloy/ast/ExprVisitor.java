package yokohama.lang.ermin.back.alloy.ast;

public interface ExprVisitor<R> {
    R visitQualNameExpr(QualNameExpr qualNameExpr);

    R visitUnOpExpr(UnOpExpr unOpExpr);

    R visitArrowExpr(ArrowExpr arrowExpr);

    R visitQuantExpr(QuantExpr quantExpr);

    R visitCompareExpr(CompareExpr compareExpr);

    R visitBinOpExpr(BinOpExpr binOpExpr);

    R visitLetExpr(LetExpr letExpr);

    R visitBlockExpr(BlockExpr blockExpr);

    R visitBoxExpr(BoxExpr boxExpr);
}
