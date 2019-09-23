package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class QualNameExpr implements Expr {
    private final QualName qualName;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        qualName.writeTo(writer, indent);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitQualNameExpr(this);
    }

    public static QualNameExpr of(String name) {
        return new QualNameExpr(QualName.of(name));
    }
}
