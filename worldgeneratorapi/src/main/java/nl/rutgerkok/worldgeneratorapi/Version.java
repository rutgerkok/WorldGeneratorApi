package nl.rutgerkok.worldgeneratorapi;

/**
 * Used to represent the version number of WorldGeneratorApi.
 *
 * @since 0.1
 */
public interface Version {

    /**
     * Checks if this API is compatible with the expected version number. If your
     * plugin expected version 1.3, and we're on 1.5, then it is likely compatible.
     * However, if your plugin expects version 2.0 and we're on 1.4, then it's not
     * going to work.
     *
     * @param maior
     *            Maior version your plugin was designed for, like 1 in "1.3".
     * @param minor
     *            Minor version your plugin was designed for, like 3 in "1.3".
     * @return True if this version is likely compatible with the provided version.
     * @since 0.1
     */
    boolean isCompatibleWith(int maior, int minor);

    /**
     * Outputs this version as a string. <strong>Do not parse this
     * string</strong>, use {@link #isCompatibleWith(int, int)} for version
     * checks instead.
     *
     * @return The version as a string.
     */
    @Override
    String toString();
}
