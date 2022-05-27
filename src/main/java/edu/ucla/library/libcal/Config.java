
package edu.ucla.library.libcal;

import static info.freelibrary.util.Constants.EMPTY;

import io.vertx.core.json.JsonObject;

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
     * The optional configuration property for the number of authentication retry attempts to make.
     */
    public static final String LIBCAL_AUTHENTICATION_RETRY_COUNT = "LIBCAL_AUTHENTICATION_RETRY_COUNT";

    /**
     * The optional configuration property for the number of seconds to wait between authentication retry attempts.
     */
    public static final String LIBCAL_AUTHENTICATION_RETRY_DELAY = "LIBCAL_AUTHENTICATION_RETRY_DELAY";

    /**
     * The optional configuration property for the number of seconds before OAuth token expiration to attempt a refresh.
     */
    @SuppressWarnings({ "PMD.LongVariable" })
    public static final String LIBCAL_AUTHENTICATION_EXPIRES_IN_PADDING = "LIBCAL_AUTHENTICATION_EXPIRES_IN_PADDING";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

    /**
     * A configuration processor that removes empty strings.
     *
     * @param aConfig An application configuration
     * @return The processed application configuration
     */
    public static JsonObject removeEmptyString(final JsonObject aConfig) {
        final JsonObject processedConfig = aConfig.copy();

        for (final String key : aConfig.fieldNames()) {
            if (aConfig.getString(key).equals(EMPTY)) {
                processedConfig.remove(key);
            }
        }

        return processedConfig;
    }
}
