package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Value;

@Value
public class Scope implements ToString {
    private final Optional<Integer> number;

    private final List<Typescope> typescopes;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append("for ");
        writer.append(number.map(n -> n.toString() + (typescopes.isEmpty() ? "" : " but ")).orElse(""));
        final boolean first = true;
        for (Typescope typescope : typescopes) {
            if (!first) {
                writer.append(", ");
            }
            typescope.writeTo(writer, indent);
        }
    }

    public static Scope of(int number) {
        return new Scope(Optional.of(number), Collections.emptyList());
    }
}
