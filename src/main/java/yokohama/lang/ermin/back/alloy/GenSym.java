package yokohama.lang.ermin.back.alloy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;
import yokohama.lang.ermin.attribute.ErminName;

@RequiredArgsConstructor
public class GenSym {
    private final Optional<GenSym> parent;

    private final Map<String, Integer> bin = new HashMap<String, Integer>();

    private final Map<ErminName, String> entityToSigName = new HashMap<>();

    private final Map<ErminName, String> entityToDeclName = new HashMap<>();

    private final Map<ErminName, String> relationshipToDeclName = new HashMap<>();

    private final Map<ErminName, String> processToPredName = new HashMap<>();

    private String stateSigName = null;

    private String initPredName = null;

    private Optional<Integer> numPrimes(String base) {
        final Integer primes = bin.get(base);
        if (primes == null) {
            return parent.flatMap(genSym -> genSym.numPrimes(base));
        } else {
            return Optional.of(primes);
        }
    }

    public String gen(String base) {
        if (base == null || base.length() == 0) {
            throw new IllegalArgumentException("base name should not be empty");
        }
        final int numPrimes = numPrimes(base).orElse(0);
        bin.put(base, numPrimes + 1);
        return base + StringUtils.repeat('\'', numPrimes);
    }

    public String genShort(String base) {
        if (base == null || base.length() == 0) {
            throw new IllegalArgumentException("base name should not be empty");
        }
        return gen(base.substring(0, 1).toLowerCase());
    }

    public String entityToSigName(ErminName entityName) {
        return entityToSigName.computeIfAbsent(entityName, key -> {
            final String sigName = gen(key.toUpperCamel());
            entityToSigName.put(key, sigName);
            return sigName;
        });
    }

    public String entityToDeclName(ErminName entityName) {
        return entityToDeclName.computeIfAbsent(entityName, key -> {
            final String declName = gen(key.toLowerCamel());
            entityToDeclName.put(key, declName);
            return declName;
        });
    }

    public String relationshipToDeclName(ErminName relationshipName) {
        return relationshipToDeclName.computeIfAbsent(relationshipName, key -> {
            final String declName = gen(key.toLowerCamel());
            relationshipToDeclName.put(key, declName);
            return declName;
        });
    }

    public String processToPredName(ErminName processName) {
        return processToPredName.computeIfAbsent(processName, key -> {
            final String predName = gen(key.toLowerCamel());
            processToPredName.put(key, predName);
            return predName;
        });
    }

    public String stateSigName() {
        if (stateSigName == null) {
            stateSigName = gen("State");
        }
        return stateSigName;
    }

    public GenSym createChild() {
        return new GenSym(Optional.of(this));
    }

    public String initPredName() {
        if (initPredName == null) {
            initPredName = gen("init");
        }
        return initPredName;
    }
}
