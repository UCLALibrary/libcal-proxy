
package edu.ucla.library.libcal;

/**
 * Properties that are used to configure the application.
 */
public final class Config {

    /**
     * The configuration property for the application's port.
     */
    public static final String HTTP_PORT = "HTTP_PORT";

    /**
     * The configuration property for the application's host.
     */
    public static final String HTTP_HOST = "HTTP_HOST";

    /**
     * The configuration property for the first LibCal client's ID credential.
     */
    public static final String OAUTH_CLIENT1_ID = "LIBCAL_CLIENT1_ID";

    /**
     * The configuration property for the first LibCal client's secret credential.
     */
    public static final String OAUTH_CLIENT1_SECRET = "LIBCAL_CLIENT1_SECRET";

    /**
     * The configuration property for the second LibCal client's ID credential.
     */
    public static final String OAUTH_CLIENT2_ID = "LIBCAL_CLIENT2_ID";

    /**
     * The configuration property for the second LibCal client's secret credential.
     */
    public static final String OAUTH_CLIENT2_SECRET = "LIBCAL_CLIENT2_SECRET";

    /**
     * The configuration property for the LibCal access token provider endpoint.
     */
    public static final String OAUTH_TOKEN_URL = "LIBCAL_TOKEN_ENDPOINT";

    /**
     * The configuration property for the LibCal access token provider endpoint.
     */
    public static final String LIBCAL_BASE_URL = "LIBCAL_BASE_URL";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

}
