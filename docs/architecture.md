# Architecture

## The chosen architecture
libcal-proxy is a [vert.x](https://vertx.io/) application.
It consists of:
* A verticle that 
** Loads application configuration
** Deploys two [services](https://vertx.io/docs/vertx-service-proxy/java/) (decribed below)
** Deploys the two request handlers
* A status handler, called to determine if the application is running
* A LibCal Proxy handler, which
** Receives LibCal API requests, determining the resource path, query string, and HTTP verb for the request
** Invokes the token service (see below) to get an OAuth token
** Invokes the proxy service (see below) to retrieve data from LibCal
** Returns the LibCal response to the original client
* A token service which
** Authenticates with the LibCal token endpoint
** Stores a pair of OAuth tokens in application shared data
** Hands off token(s) to the proxy service (see below)
** Periodically polls LibCal to refresh the tokens
* A proxy service which
** Calls LibCal, using the path/query/verb identified by the proxy handler
** Returns the response (status code/message, response body) to the proxy handler


## Functional Overview
The proxy exists to insulate cients from the process of retrieving an OAuth token from the LibCal OAuth provider. Client calls are identical to direct calls to a LibCal API, except for two points: 
1) "proxy" is prefixed to the UCLA LibCal URL (e.g.: if the direct LibCal call is https://calendar.library.ucla.edu/1.1/event_search?search=*, the proxy call would be https://proxy.calendar.library.ucla.edu/1.1/event_search?search=*)
2) No Authorization header is constructed by the client

Otherwise, the client builds a request URL and submits is via the same HTTP verb that would be used in a direct call to LibCal.
The proxy service maintains a pool of OAuth tokens, periodically polled to maintain fresh tokens (LibCal tokens expire 1 hour after creation). The proxy builds an Authorization header with a stored token, then forwards the request (including request body for POST requests) to LibCal. No validation is performed on the client request. The LibCal response is then returned to the client (except in the cases of system errors, e.g., failure to connect to LibCal).

For security, client calls are screeneed by client IP: client IPs must fall within configured ranges, elsewise a 403 Forbidden response will be returned.
