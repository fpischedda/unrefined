# ROADMAP

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
