package yokohama.lang.ermin.relationship;

public interface ErminRelationshipExpVisitor<R> {

    R visitAtomicRelationshipExp(ErminAtomicRelationshipExp atomicRelationshipExp);

    R visitProductRelationshipExp(ErminProductRelationshipExp productRelationshipExp);
}
