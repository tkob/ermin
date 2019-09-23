package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class PredDecl implements Paragraph {
    private final Optional<QualName> qualName;

    private final String name;

    private final List<Decl> paraDecls;

    private final Block block;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(StringUtils.repeat(' ', indent));
        writer.append("pred ");
        writer.append(name);
        writer.append(" (");
        boolean first = true;
        for (Decl paraDecl : paraDecls) {
            if (!first) {
                writer.append(", ");
            }
            paraDecl.writeTo(writer, indent);
            first = false;
        }
        writer.append(") ");
        block.writeTo(writer, indent + 4);
    }

    @Override
    public <R> R accept(ParagraphVisitor<R> visitor) {
        return visitor.visitPredDecl(this);
    }

    public static PredDecl of(String name, List<Decl> paraDecls, Block block) {
        return new PredDecl(Optional.empty(), name, paraDecls, block);
    }
}
