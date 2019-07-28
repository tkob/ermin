package yokohama.lang.ermin.attribute;

import lombok.Value;

@Value
public class ErminName {
    private String name;

    public String toString() {
        return name;
    }
}
