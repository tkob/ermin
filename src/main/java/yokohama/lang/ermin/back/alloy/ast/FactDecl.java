package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class FactDecl implements Paragraph {
    private final Optional<String> name;

    private final Block block;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(StringUtils.repeat(' ', indent));
        writer.append("fact ");
        writer.append(name.map(n -> n + " ").orElse(""));
        block.writeTo(writer, indent + 4);
    }

    @Override
    public <R> R accept(ParagraphVisitor<R> visitor) {
        return visitor.visitFactDecl(this);
    }

    public static FactDecl of(String name, Block block) {
        return new FactDecl(Optional.of(name), block);
    }

    public static FactDecl of(Block block) {
        return new FactDecl(Optional.empty(), block);
    }
}
