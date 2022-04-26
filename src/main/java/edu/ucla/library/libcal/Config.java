
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
     * The configuration property for the LibCal client ID credential.
     */
    public static final String OAUTH_CLIENT_ID = "LIBCAL_CLIENT_ID"; //"libcal.oauth.client.id";

    /**
     * The configuration property for the LibCal client secret credential.
     */
    public static final String OAUTH_CLIENT_SECRET = "LIBCAL_SECRET"; //"libcal.oauth.client.secret";

    /**
     * The configuration property for the LibCal access token provider endpoint.
     */
    public static final String OAUTH_TOKEN_URL = "LIBCAL_TOKEN_ENDPOINT"; //"libcal.oauth.provider.url";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

}
