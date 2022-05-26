# LibCal-Proxy

The LibCal-Proxy is a simple proxy-ish service that sits in front of LibCal and handles API authorization for other applications that do not know how to use OAuth. Instead, these other applications' IP addresses are checked to confirm that they should have access to the LibCal APIs.

## Client usage

Clients are expected to send HTTP requests to this proxy service as if they were connecting to LibCal directly, except that the Authorization HTTP request header may be omitted. See the LibCal API [usage guide](https://calendar.library.ucla.edu/admin/api/usage-guide) and [API docs](https://calendar.library.ucla.edu/admin/api) for more information.

## Building the Project

To build the project, which includes running a bunch of tests, type:

    mvn verify \
        -Dlibcal.client.id="123" \
        -Dlibcal.secret="0123456789abcdef0123456789abcdef" \
        -Dlibcal.token.endpoint="https://calendar.library.ucla.edu/1.1/oauth/token"
        -Dlibcal.base.url="https://calendar.library.ucla.edu"

## Running in Development

The easiest way to run the application locally for testing is to use Maven:

    LIBCAL_CLIENT_ID=123 \
    LIBCAL_SECRET=0123456789abcdef0123456789abcdef \
    LIBCAL_TOKEN_ENDPOINT=https://calendar.library.ucla.edu/1.1/oauth/token \
    LIBCAL_BASE_URL=https://calendar.library.ucla.edu \
    mvn vertx:run

If you want to run the appliction in its Docker container, you can also do that through Maven:

    mvn initialize docker:run \
        -Dlibcal.client.id="123" \
        -Dlibcal.secret="0123456789abcdef0123456789abcdef" \
        -Dlibcal.token.endpoint="https://calendar.library.ucla.edu/1.1/oauth/token"
        -Dlibcal.base.url="https://calendar.library.ucla.edu"

This will run the Docker image in the foreground, with the logs being displayed in the terminal. You can use Ctrl-C to
stop it.

If you'd like to run it in the background, use:

    mvn initialize docker:start \
        -Dlibcal.client.id="123" \
        -Dlibcal.secret="0123456789abcdef0123456789abcdef" \
        -Dlibcal.token.endpoint="https://calendar.library.ucla.edu/1.1/oauth/token"
        -Dlibcal.base.url="https://calendar.library.ucla.edu"

When you're ready to stop it, you can type:

    mvn docker:stop

The build will also create a Docker image in your local Docker image repository, so one could also, of course, run it
from there using other Docker tooling. 

## Contact

We use an internal ticketing system, but have left the GitHub [issues](https://github.com/UCLALibrary/libcal-proxy/issues)
open in case you'd like to file a ticket or make a suggestion. Thanks for checking the project out!

