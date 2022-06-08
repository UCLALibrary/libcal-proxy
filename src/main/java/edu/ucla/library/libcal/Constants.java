
package edu.ucla.library.libcal;

/**
 * A class for package constants.
 */
public final class Constants {

    /**
     * The name of the shared data map that contains the access token.
     */
    public static final String ACCESS_TOKEN_MAP = "accessTokenMap";

    /**
     * The key of the access token stored in the shared data map.
     */
    public static final String ACCESS_TOKEN = "accessToken";

    /**
     * The name of the path parameter holding the LibCal query
     */
    public static final String QUERY_PARAM = "theQuery";

    /**
     * Just a empty string, useful.
     */
    public static final String EMPTY = "";

    /**
     * Slash character used in URL assembly
     */
    public static final String SLASH = "/";

    /**
     * The IP for an unspecified host.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String UNSPECIFIED_HOST = "0.0.0.0";

    /**
     * The IP for an unspecified host.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String LOCAL_HOST = "127.0.0.1";

    /**
     * HTTP 200 status phrase.
     */
    public static final String OK = "OK";

    /**
     * A constant for the break tag.
     */
    public static final String BR_TAG = "<br>";

    /**
     * A regular expression representing end of line character(s).
     */
    public static final String EOL_REGEX = "\\r|\\n|\\r\\n";

    /**
     * Constant classes should have private constructors.
     */
    private Constants() {
        // This is intentionally left empty
    }

}
