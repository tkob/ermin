package yokohama.lang.ermin.front;

import java.io.InputStreamReader;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import yokohama.lang.ermin.Yylex;
import yokohama.lang.ermin.parser;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.type.ErminDecimalType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminVarCharType;

public class TypeResolverTest {
    CodeResolverFactory codeResolverFactory = new CodeResolverFactory();

    TypeResolverFactory typeResolverFactory = new TypeResolverFactory();

    private Top parse(String path) throws Exception {
        final Yylex l = new Yylex(new InputStreamReader(this.getClass().getResourceAsStream(path)));
        final parser p = new parser(l);

        return p.pTop();
    }

    @Test
    public void testNormal() throws Exception {
        final Top top = parse("TypeResolverTest_normal.ermin");
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);
        final TypeResolver typeResolver = typeResolverFactory.fromAbsyn(top, codeResolver);

        Assert.assertEquals(Optional.of(new ErminDecimalType(9, 2)),
                            typeResolver.resolve(ErminName.fromSnake("rate")));
        Assert.assertEquals(Optional.of(new ErminDecimalType(9, 2)),
                            typeResolver.resolve(ErminName.fromSnake("exchange_rate")));
        Assert.assertEquals(Optional.of(new ErminVarCharType(40)),
                            typeResolver.resolve(ErminName.fromSnake("name")));
        Assert.assertEquals(Optional.of(new ErminVarCharType(40)),
                            typeResolver.resolve(ErminName.fromSnake("product_name")));
        Assert.assertEquals(Optional.of(new ErminIntegerType()),
                            typeResolver.resolve(ErminName.fromSnake("id")));
        Assert.assertEquals(Optional.of(new ErminIntegerType()),
                            typeResolver.resolve(ErminName.fromSnake("person_id")));
        Assert.assertEquals(Optional.of(new ErminIntegerType()),
                            typeResolver.resolve(ErminName.fromSnake("employee_id")));
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicate() throws Exception {
        final Top top = parse("TypeResolverTest_dupulicate.ermin");
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);
        typeResolverFactory.fromAbsyn(top, codeResolver);
    }

    @Test(expected = RuntimeException.class)
    public void testCycle1() throws Exception {
        final Top top = parse("TypeResolverTest_cycle1.ermin");
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);

        typeResolverFactory.fromAbsyn(top, codeResolver);
    }

    @Test(expected = RuntimeException.class)
    public void testCycle2() throws Exception {
        final Top top = parse("TypeResolverTest_cycle2.ermin");
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);
        typeResolverFactory.fromAbsyn(top, codeResolver);
    }

}
