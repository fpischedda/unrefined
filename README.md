# Unrefined
A simple ticket estimation tool

_Unrefined_ aims to provide a dead simple flow to estimate tickets

## Overview

Product guy:
- Lands to the home page
- Inserts the ticket id and creates an estimation session
- Sends the link to the estimation page
- Wait for estimations to come from engineers
- Show results

Engineer:
- Land to the estimation page
- Write their name and vote (or skip)
- Wait for the final result

Repeat...

## Disclaimer

This is a VERY primitive software, lots of stuff is missing like authentication,
durable storage and a good UI; nothing stops you to trash it in favor of more
mature solutions, but you are free to extend it to fit your needs.

This is nothing more than an experiment so don't expect more from it.

## Development

You will need clojure build tools and a editor that support REPL driven development; popular options:

- Emacs + CIDER
- VSCode + Calva
- (Neo)Vim + Conjure or Fireplace
- Cursive
- Others...

### Create a standalone jar

To create a production build run:

	clj -T:build uber

A new version of the standalone jar will be available in the target/ directory.

### Running the server locally

To run the server locally (including nrepl):

	clj -M:run

It will start the http server (default port 8080) and the nrepl server (default port 1667), happy hacking!

## Configuration

The server can be configured by setting few environment variables:
- UNREFINED_HTTP_PORT: the port the web server will listen to
- UNREFINED_NREPL_PORT: the port to connect with an nrepl client

## License

Copyleft © 2022 Francesco Pischedda

Distributed under the AGPL v3, see LICENCE file for more details
