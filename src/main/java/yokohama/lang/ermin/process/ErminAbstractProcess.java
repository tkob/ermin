package yokohama.lang.ermin.process;

import java.util.List;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminAbstractProcess {
    private final ErminName name;

    private final List<ErminArgument> arguments;

    private final List<ErminStatement> statements;
}
