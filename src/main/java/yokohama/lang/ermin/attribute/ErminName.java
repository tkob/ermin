package yokohama.lang.ermin.attribute;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Value;

@Value
public class ErminName {

    private List<String> parts;

    public String toString() {
        return parts.stream().collect(Collectors.joining("_"));
    }

    public static ErminName fromSnake(String localName) {
        return new ErminName(Arrays.asList(localName.split("_+")));
    }

}
