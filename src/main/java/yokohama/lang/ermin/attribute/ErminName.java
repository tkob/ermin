package yokohama.lang.ermin.attribute;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Value;

@Value
public class ErminName {
    private Optional<List<String>> nameSpace;

    private List<String> localNameParts;

    public String toString() {
        return nameSpace.map(ns -> ns.stream().collect(Collectors.joining("_")) + "::")
                .orElse("") + localNameParts.stream().collect(Collectors.joining("_"));
    }

    public static ErminName fromSnakeNS(String nameSpace, String localName) {
        return new ErminName(Optional.of(Arrays.asList(nameSpace.split("_+"))), Arrays
                .asList(localName.split("_+")));
    }

    public static ErminName fromSnake(String localName) {
        return new ErminName(Optional.empty(), Arrays.asList(localName.split("_+")));
    }

}
