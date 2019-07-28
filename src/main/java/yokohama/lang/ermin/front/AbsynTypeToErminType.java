package yokohama.lang.ermin.front;

import yokohama.lang.ermin.Absyn.Type;
import yokohama.lang.ermin.type.ErminBlobType;
import yokohama.lang.ermin.type.ErminCharType;
import yokohama.lang.ermin.type.ErminClobType;
import yokohama.lang.ermin.type.ErminDateType;
import yokohama.lang.ermin.type.ErminDecimalType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminType;
import yokohama.lang.ermin.type.ErminVarCharType;

public class AbsynTypeToErminType implements Type.Visitor<ErminType, TypeResolver> {

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.CharType p,
            TypeResolver typeResolver) {
        return new ErminCharType(p.integer_);
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.VarCharType p,
            TypeResolver typeResolver) {
        return new ErminVarCharType(p.integer_);
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.ClobType p,
            TypeResolver typeResolver) {
        return new ErminClobType();
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.BlobType p,
            TypeResolver typeResolver) {
        return new ErminBlobType();
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.DecimalType p,
            TypeResolver typeResolver) {
        return new ErminDecimalType();
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.DecimalPrecisionType p,
            TypeResolver typeResolver) {
        return new ErminDecimalType(p.integer_);
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.DecimalPrecisionScaleType p,
            TypeResolver typeResolver) {
        return new ErminDecimalType(p.integer_1, p.integer_2);
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.IntegerType p,
            TypeResolver typeResolver) {
        return new ErminIntegerType();
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.DateType p,
            TypeResolver typeResolver) {
        return new ErminDateType();
    }

    @Override
    public ErminType visit(yokohama.lang.ermin.Absyn.IdentType p,
            TypeResolver typeResolver) {
        return typeResolver.resolveOrThrow(p.ident_);
    }

}
