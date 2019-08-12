package yokohama.lang.ermin.front;

public interface CodeResolver extends Resolver<String, Iterable<String>> {
    Iterable<String> getNames();
}
