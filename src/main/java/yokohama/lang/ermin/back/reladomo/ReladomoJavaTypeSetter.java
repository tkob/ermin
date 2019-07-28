package yokohama.lang.ermin.back.reladomo;

import java.math.BigDecimal;
import java.sql.Date;

import lombok.RequiredArgsConstructor;
import yokohama.lang.ermin.type.ErminBlobType;
import yokohama.lang.ermin.type.ErminCharType;
import yokohama.lang.ermin.type.ErminClobType;
import yokohama.lang.ermin.type.ErminStringCodeType;
import yokohama.lang.ermin.type.ErminDateType;
import yokohama.lang.ermin.type.ErminDecimalType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminTypeVisitor;
import yokohama.lang.ermin.type.ErminVarCharType;
import yokohama.lang.reladomo.AttributePureType;

@RequiredArgsConstructor
public class ReladomoJavaTypeSetter implements ErminTypeVisitor<Void> {

    private final AttributePureType attributePure;

    // valid types in Reladomo are
    // [boolean, double, byte, char, short, float, int, long]
    // or [byte[], Time, String, Timestamp, Date, BigDecimal]

    @Override
    public Void visitCharType(ErminCharType charType) {
        attributePure.setJavaType(String.class.getSimpleName());
        return null;
    }

    @Override
    public Void visitVarCharType(ErminVarCharType varCharType) {
        attributePure.setJavaType(String.class.getSimpleName());
        attributePure.setMaxLength(varCharType.getSize());
        return null;
    }

    @Override
    public Void visitClobType(ErminClobType clobType) {
        attributePure.setJavaType(String.class.getSimpleName());
        return null;
    }

    @Override
    public Void visitBlobType(ErminBlobType blobType) {
        attributePure.setJavaType(byte[].class.getSimpleName());
        return null;
    }

    @Override
    public Void visitDecimalType(ErminDecimalType decimalType) {
        attributePure.setJavaType(BigDecimal.class.getSimpleName());
        decimalType.getPrecision().ifPresent(precision -> {
            attributePure.setPrecision(precision);
            decimalType.getScale().ifPresent(scale -> {
                attributePure.setScale(scale);
            });
        });
        return null;
    }

    @Override
    public Void visitIntegerType(ErminIntegerType integerType) {
        attributePure.setJavaType(int.class.getSimpleName());
        return null;
    }

    @Override
    public Void visitDateType(ErminDateType dateType) {
        attributePure.setJavaType(Date.class.getSimpleName());
        return null;
    }

    @Override
    public Void visitStringCodeType(ErminStringCodeType dateType) {
        attributePure.setJavaType(String.class.getSimpleName());
        return null;
    }

}
