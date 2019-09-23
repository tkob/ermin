package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class BlockExpr implements Expr {
    private final Block block;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        block.writeTo(writer, indent);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitBlockExpr(this);
    }

    public static BlockExpr of(Expr... exprs) {
        return new BlockExpr(Block.of(exprs));
    }
}
