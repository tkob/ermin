package yokohama.lang.ermin.process;

import yokohama.lang.ermin.attribute.ErminName;

public interface ErminArgument {
    String getVarName();

    ErminName getEntityName();

    <R> R accept(ErminArgumentVisitor<R> visitor);
}
