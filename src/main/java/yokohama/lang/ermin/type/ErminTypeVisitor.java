package yokohama.lang.ermin.type;

public interface ErminTypeVisitor<R> {

    R visitCharType(ErminCharType charType);

    R visitVarCharType(ErminVarCharType varCharType);

    R visitClobType(ErminClobType clobType);

    R visitBlobType(ErminBlobType blobType);

    R visitDecimalType(ErminDecimalType decimalType);

    R visitIntegerType(ErminIntegerType integerType);

    R visitDateType(ErminDateType dateType);

    R visitStringCodeType(ErminStringCodeType stringCodeType);
}
