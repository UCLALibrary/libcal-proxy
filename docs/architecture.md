# Architecture

## The chosen architecture
libcal-proxy is a [vert.x](https://vertx.io/) application.
It consists of:
* A verticle that 
** Loads application configuration
** Retrieves initial OAuth tokens
** Deploys the two request handlers
* A status handler, called to determine if the application is running
* A LibCal Proxy handler, which
** Receives LibCal API requests
** Retrieves an OAuth token from application storage
** Passes the API request along to LibCal with the token
** Returns the LibCal response to the original client
