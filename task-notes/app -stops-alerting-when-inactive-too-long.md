# Description

The app stops sending inactivity alerts when the user ignores the alerts for a while.

Ideally when the user ignores the alerts the alerts should progressively become more strident!

# Hypothesis: the OS is preventing the app from running

## Suspected Cause

1) The app is actively performing background processing
2) The user is not interacting with the app

To save batter the OS is preventing the app from running.

## Proposed solution

Create a foreground Service displaying a notification so that the OS
will allow the app to run in the background.

See https://developer.android.com/guide/components/foreground-services