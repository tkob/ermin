package yokohama.lang.ermin.front;

import yokohama.lang.ermin.attribute.ErminName;

public interface CodeResolver extends Resolver<ErminName, Iterable<String>> {
    Iterable<ErminName> getNames();

    int maxLength(ErminName name);
}
