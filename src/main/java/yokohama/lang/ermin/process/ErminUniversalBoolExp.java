package yokohama.lang.ermin.process;

import java.util.List;

import lombok.Value;

@Value
public class ErminUniversalBoolExp implements ErminBoolExp {
    private final List<ErminDecl> decls;

    private final ErminBoolExp body;

    @Override
    public <R> R accept(ErminBoolExpVisitor<R> visitor) {
        return visitor.visitUniversalBoolExp(this);
    }
}
