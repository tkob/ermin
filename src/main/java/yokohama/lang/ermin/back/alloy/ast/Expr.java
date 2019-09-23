package yokohama.lang.ermin.back.alloy.ast;

public interface Expr extends ToString {
    <R> R accept(ExprVisitor<R> visitor);
}
