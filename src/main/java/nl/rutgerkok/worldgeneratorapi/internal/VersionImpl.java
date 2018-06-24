package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import nl.rutgerkok.worldgeneratorapi.Version;

/**
 * Class for parsing string-based versions.
 *
 */
public final class VersionImpl implements Version {

    private final String version;
    private final int maior;
    private final int minor;

    public VersionImpl(String version) {
        this.version = Objects.requireNonNull(version, "version");

        String[] split = version.replace("-SNAPSHOT", "").split("\\.");
        maior = Integer.parseInt(split[0]);
        minor = Integer.parseInt(split[1]);
    }

    @Override
    public boolean isCompatibleWith(int maior, int minor) {
        if (this.maior > maior) {
            // Maybe this needs to be changed if 1.0 is largely incompatible with 0.1
            return true;
        }
        return this.maior == maior && this.minor >= minor;
    }

    @Override
    public String toString() {
        return version;
    }
}
