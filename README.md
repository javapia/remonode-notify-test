# remonode-notify-test

Scratch repository for exercising remonode's **GitHub Push** trigger end to end:
a push here should reach the trigger's endpoint, match the repo/branch filter,
and start the workflow on the selected devices.

- Repository field in the trigger: `javapia/remonode-notify-test`
- Branch field: `main` (blank would run on a push to any branch)

Pushes to this repo carry no meaning beyond "an event happened".

- test1
- test2
