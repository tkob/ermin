package yokohama.lang.ermin.front;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import yokohama.lang.ermin.Absyn.AbstractProcessDef;
import yokohama.lang.ermin.Absyn.ChildEntityDef;
import yokohama.lang.ermin.Absyn.Code;
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.GuardedProcessDef;
import yokohama.lang.ermin.Absyn.KeyOnlyChildEntityDef;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.StringCode;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.attribute.ErminName;

public class CodeResolverFactory {

    public CodeResolver fromAbsyn(Top top) {
        return top.accept(new Top.Visitor<CodeResolver, Void>() {

            @Override
            public CodeResolver visit(TopDefinitions p, Void arg) {
                final Stream<CodeDef> codeDefs = filterCodeDef(p.listdef_.stream());
                return fromCodeDefs(codeDefs.collect(Collectors.toList()));
            }
        }, null);
    }

    public CodeResolver fromCodeDefs(Iterable<CodeDef> codeDefs) {
        final Set<String> codeNames = new HashSet<String>();
        codeDefs.forEach(codeDef -> {
            final String codeName = codeDef.ident_;
            if (codeNames.contains(codeName)) {
                throw new RuntimeException("duplicate code definition: " + codeName);
            } else {
                codeNames.add(codeName);
            }
        });

        final Map<ErminName, Iterable<String>> nameToValues = new HashMap<>();
        codeDefs.forEach(codeDef -> {
            nameToValues.put(ErminName.fromSnake(codeDef.ident_),
                             codeDef.listcode_.stream()
                                              .map(code -> code.accept(new Code.Visitor<String, Void>() {

                                                  @Override
                                                  public String visit(StringCode p, Void arg) {
                                                      return p.string_;
                                                  }

                                              }, null))
                                              .collect(Collectors.toList()));
        });

        return new CodeResolver() {

            @Override
            public Optional<Iterable<String>> resolve(ErminName name) {
                return Optional.ofNullable(nameToValues.get(name));
            }

            @Override
            public Iterable<ErminName> getNames() {
                return nameToValues.keySet();
            }

            @Override
            public int maxLength(ErminName name) {
                final Stream<String> codes =
                    StreamSupport.stream(this.resolveOrThrow(name).spliterator(), false);
                return codes.max(new Comparator<String>() {
                    @Override
                    public int compare(String code1, String code2) {
                        return Integer.compare(code1.length(), code2.length());
                    }
                }).map(code -> code.length()).orElse(0);
            }
        };

    }

    public Stream<CodeDef> filterCodeDef(Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<CodeDef>, Void>() {

            @Override
            public Stream<CodeDef> visit(TypeDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(CodeDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<CodeDef> visit(ChildEntityDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(KeyOnlyChildEntityDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(EntityDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(RelationshipDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }

            @Override
            public Stream<CodeDef> visit(GuardedProcessDef p, Void arg) {
                return Stream.<CodeDef> empty();
            }
        }, null));
    }

}
