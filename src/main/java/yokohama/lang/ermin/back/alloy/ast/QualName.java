package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import lombok.Value;

@Value
public class QualName implements QualNameOrBlock {
    private final boolean thisPrefix;

    private final List<String> prefixes;

    private final String name;

    @Override
    public <R> R accept(QualNameOrBlockVisitor<R> visitor) {
        return visitor.visitQualName(this);
    }

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        if (thisPrefix) {
            writer.append("this/");
        }
        for (String prefix : prefixes) {
            writer.append(prefix);
            writer.append('/');
        }
        writer.append(name);
    }

    public static QualName of(String name) {
        return new QualName(false, Collections.emptyList(), name);
    }
}
