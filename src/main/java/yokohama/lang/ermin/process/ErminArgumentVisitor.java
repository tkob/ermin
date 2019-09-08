package yokohama.lang.ermin.process;

public interface ErminArgumentVisitor<R> {
    R visitNewEntityArgument(ErminNewEntityArgument newEntityArgument);

    R visitExistingEntityArgument(ErminExistingEntityArgument existingEntityArgument);
}
