# ROADMAP

## Update - 2023-01-06

From now on new goals and improvements will inserted on top of this file.

Changes introduced so far to the MVP:

- Voting UI reworked to use sliders for votes and show a voting cheatsheet
- Refinement and ticket data is now persisted using datahike
- Close and event sources periodically when these have no connected clients

Next steps in no particular order:

- Validate that voter name exists when voting (not useful for skipping)
- Create some introduction content to describe the project (either video or blog)
- More testing
- Add contact info and link to sources
- Show this thing around to get feedback

## Features of the MVP

- Create a refinement session with at least a ticket to estimate
- Share the link to the estimation page
- Estimate a ticket, providing story points or skipping the estimation
  - Write the breakdown of the provided story points
- Show the result of the estimation
  - Provide a way to re-estimate a ticket
- Estimate a new ticket directly from the result or watch pages

As of commit [672416a](https://github.com/fpischedda/unrefined/commit/672416a3f8e5680765e4c3727c0ff7068422c36f) the MVP is considered as complete.
Focus will now be on better user and developer experience, possibly adding useful features.

## Ideas for future improvements

- Friendly development environment especially to modify frontend code, ideas:
  - MAYBE Dockerfile that runs the server
  - DONE Alias in deps.edn to run the server
- Change the estimation page to force the use of the breakdown form
  - DONE The total will be calculated summing the breakdowns
  - It would be nice to be able to configure the breakdown items
  - MAYBE It would be nice to be able to enable breakdowns or the single estimation
- Better design and UX
  - DONE responsive design (at least is kind of usable on a mobile device)
- Persistent storage
  - DONE Some ideas [here](https://github.com/fpischedda/unrefined/issues/10)
