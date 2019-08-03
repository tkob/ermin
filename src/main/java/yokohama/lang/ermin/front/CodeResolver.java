package yokohama.lang.ermin.front;

public interface CodeResolver extends Resolver<Iterable<String>> {
    Iterable<String> getNames();
}
