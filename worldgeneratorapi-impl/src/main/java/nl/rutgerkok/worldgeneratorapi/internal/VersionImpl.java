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
        if (maior == 1 && minor < 3) {
            // Don't accept anything that was built for 1.0 to 1.2
            return false;
        }
        if (maior < 1) {
            // Don't accept anything that was built for 0.x
            return false;
        }
        return this.maior == maior && this.minor >= minor;
    }

    @Override
    public String toString() {
        return version;
    }
}
