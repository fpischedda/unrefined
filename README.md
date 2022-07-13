# unrefined
A simple ticket estimation tool

__Unrefined_ aims to provide a dead simple flow to estimate tickets

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

## Development

You will need clojure build tools and a editor that support REPL driven development; popular options:

- Emacs + CIDER
- VSCode + Calva
- (Neo)Vim + Conjure or Fireplace
- Cursive
- Others...

To create a production build run:

	clj -T:build uber

A new version of the standalone jar will be available in the target/ directory.

## Configuration

The server can be configured by setting few environment variables:
- UNREFINED_HTTP_PORT: the port the web server will listen to
- UNREFINED_NREPL_PORT: the port to connect with an nrepl client
- UNREFINED_LINK_TO_TICKET: a format string used to create a link to your ticket system, for ex. http://your.ticketing-system.whatever/ticket/%s

## License

Copyleft © 2022 Francesco Pischedda

Distributed under the AGPL v3, see LICENCE file for more details
