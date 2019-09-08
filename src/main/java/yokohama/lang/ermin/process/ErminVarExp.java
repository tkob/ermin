package yokohama.lang.ermin.process;

import lombok.Value;

@Value
public class ErminVarExp implements ErminExp {
    private final String name;

    public <R> R accept(ErminExpVisitor<R> visitor) {
        return visitor.visitVarExp(this);
    }

}
