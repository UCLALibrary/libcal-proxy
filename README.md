# LibCal-Proxy

The LibCal-Proxy is a simple proxy-ish service that sits in front of LibCal and handles API authorization for other services that do not know how to use OAuth. Instead, these other services' IP addresses are checked to confirm that they should have access to the LibCal APIs.

## Building the Project

To build the project, which includes running a bunch of tests, type:

    mvn verify

## Running in Development

The easiest way to run the service locally for testing is to use Maven:

    mvn vertx:run

An alternative route though is to run Docker manually. After running a build, the Docker image will be in your local
Docker repository. To run it, just type the following:

    docker run -p 8888:8888 libcal-proxy

Alternatively, the process can be put in the background with:

    docker run -d -p 8888:8888 libcal-proxy

## Contact

We use an internal ticketing system, but have left the GitHub [issues](https://github.com/UCLALibrary/fester/issues)
open in case you'd like to file a ticket or make a suggestion. Thanks for checking the project out!

