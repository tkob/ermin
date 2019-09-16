package yokohama.lang.ermin.attribute;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class ErminName {

    private List<String> parts;

    public String toString() {
        return parts.stream().collect(Collectors.joining("_"));
    }

    public String toUpperCamel() {
        return parts.stream().map(part -> StringUtils.capitalize(part.toLowerCase()))
                .collect(Collectors.joining());
    }

    public String toLowerCamel() {
        return StringUtils.uncapitalize(toUpperCamel());
    }

    public String toSnake() {
        return String.join("_", parts);
    }

    public String toLowerSnake() {
        return parts.stream().map(part -> part.toLowerCase())
                .collect(Collectors.joining("_"));
    }

    public static ErminName fromSnake(String snake) {
        return new ErminName(Arrays.asList(snake.split("_", -1)));
    }

}
