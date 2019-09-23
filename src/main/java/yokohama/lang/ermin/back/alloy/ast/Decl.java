package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Value;

@Value
public class Decl implements ToString {

    private final boolean nameDisj;

    private final List<String> names;

    private final boolean exprDisj;

    private final Expr expr;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        if (nameDisj) {
            writer.append("disj ");
        }
        writer.append(names.stream().collect(Collectors.joining(", ")));
        writer.append(": ");
        if (exprDisj) {
            writer.append("disj ");
        }
        expr.writeTo(writer, indent);
    }

    public static Decl of(String name, Expr expr) {
        return new Decl(false, Arrays.asList(name), false, expr);
    }
}
