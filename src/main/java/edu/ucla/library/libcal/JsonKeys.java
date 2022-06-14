
package edu.ucla.library.libcal;

/**
 * A collection of JSON keys.
 */
public final class JsonKeys {

    /**
     * A status key.
     */
    public static final String STATUS = "status";

    /**
     * A status key.
     */
    public static final String ERROR = "error";

    /**
     * An OAuth access token key.
     */
    public static final String ACCESS_TOKEN = "access_token";

    /**
     * An OAuth expires in key.
     */
    public static final String EXPIRES_IN = "expires_in";

    /**
     * The status code returned with an HTTP response.
     */
    public static final String STATUS_CODE = "status_code";

    /**
     * The status message returned with an HTTP response.
     */
    public static final String STATUS_MESSAGE = "status_message";

    /**
     * The body of an HTTP response.
     */
    public static final String BODY = "body";

    /**
     * Creates a new JSON keys constants class.
     */
    private JsonKeys() {
        // This is intentionally left empty
    }

}
