# LibCal-Proxy

The LibCal-Proxy is a simple proxy-ish service that sits in front of LibCal and handles API authorization for other applications that do not know how to use OAuth. Instead, these other applications' IP addresses are checked to confirm that they should have access to the LibCal APIs.

## Client usage

Clients are expected to send HTTP requests to this proxy service as if they were connecting to LibCal directly, except that the Authorization HTTP request header may be omitted. See the LibCal API [usage guide](https://calendar.library.ucla.edu/admin/api/usage-guide) and [API docs](https://calendar.library.ucla.edu/admin/api) for more information.

## Building the Project

To build the project, which includes running a bunch of tests, type:

    mvn verify \
        -Dlibcal.client1.id="123" \
        -Dlibcal.client1.secret="0123456789abcdef0123456789abcdef" \
        -Dlibcal.client2.id="456" \
        -Dlibcal.client2.secret="0123456789abcdef0123456789abcdef" \
        -Dlibcal.token.endpoint="https://calendar.library.ucla.edu/1.1/oauth/token" \
        -Dlibcal.base.url="https://calendar.library.ucla.edu" \
        -Dlibcal.allowed.ips="127.0.0.0/16" # A comma-separated list of IPv4 CIDR blocks
                                            # Note that single IP addresses must be suffixed with /32

To avoid typing these values each time, they can also be stored in a [Maven settings.xml file](https://maven.apache.org/settings.html#properties) using the following properties:

        <libcal.client1.id>123</libcal.client1.id>
        <libcal.client2.id>456</libcal.client2.id>
        <libcal.client1.secret>0123456789abcdef0123456789abcdef</libcal.client1.secret>
        <libcal.client2.secret>0123456789abcdef0123456789abcdef</libcal.client2.secret>
        <libcal.token.endpoint>https://calendar.library.ucla.edu/1.1/oauth/token</libcal.token.endpoint>
        <libcal.base.url>https://calendar.library.ucla.edu</libcal.base.url>
        <libcal.allowed.ips>127.0.0.0/16</libcal.allowed.ips>

To find the `libcal.client*` values, check your LibCal instance; you may need to create them if they do not exist.

For authenticated UCLA people, these can be found at https://calendar.library.ucla.edu/admin/api/authentication.

## Running in Development

To run the application in a Docker container, use `mvn initialize docker:run`, passing in the configuration via Maven properties as above. This will run the container in the foreground, with the logs being displayed in the terminal. You can use Ctrl-C to stop it.

If you'd like to run it in the background, use `docker:start` instead of `docker:run`. When you're ready to stop it, you can type:

    mvn docker:stop

The build will also create a Docker image in your local Docker image repository, so one could also, of course, run it
from there using other Docker tooling.

You can also run the application directly on the host machine by defining environment variables instead of Maven properties:

    LIBCAL_CLIENT1_ID=123 \
    LIBCAL_CLIENT1_SECRET=0123456789abcdef0123456789abcdef \
    LIBCAL_CLIENT2_ID=456 \
    LIBCAL_CLIENT2_SECRET=0123456789abcdef0123456789abcdef \
    LIBCAL_TOKEN_ENDPOINT=https://calendar.library.ucla.edu/1.1/oauth/token \
    LIBCAL_BASE_URL=https://calendar.library.ucla.edu \
    LIBCAL_ALLOWED_IPS="127.0.0.0/16" \
    mvn vertx:run

If you want to fine tune the application's configuration, any of the additional options below can also be supplied:

    LIBCAL_AUTH_RETRY_COUNT=3 \
    LIBCAL_AUTH_RETRY_DELAY=10 \
    LIBCAL_AUTH_EXPIRES_IN_PADDING=300 \

## Contact

We use an internal ticketing system, but have left the GitHub [issues](https://github.com/UCLALibrary/libcal-proxy/issues)
open in case you'd like to file a ticket or make a suggestion. Thanks for checking the project out!

