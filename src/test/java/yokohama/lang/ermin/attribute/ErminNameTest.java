package yokohama.lang.ermin.attribute;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class ErminNameTest {

    @Test
    public void testToUpperCamel() {
        final ErminName sut = ErminName.fromSnake("snake_case");

        Assert.assertEquals("SnakeCase", sut.toUpperCamel());
    }

    @Test
    public void testToLowerCamel() {
        final ErminName sut = ErminName.fromSnake("snake_case");

        Assert.assertEquals("snakeCase", sut.toLowerCamel());
    }

    @Test
    public void testToSnake() {
        final ErminName sut = new ErminName(Arrays.asList("part", "Part"));

        Assert.assertEquals("part_Part", sut.toSnake());
    }

    @Test
    public void testToLowerSnake() {
        final ErminName sut = new ErminName(Arrays.asList("part", "Part"));

        Assert.assertEquals("part_part", sut.toLowerSnake());
    }

}
