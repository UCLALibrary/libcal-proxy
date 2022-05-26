
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
     * Constant classes should have private constructors.
     */
    private Constants() {
        // This is intentionally left empty
    }

}
