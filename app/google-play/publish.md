Build release APK
=================
1. In Android Studio, use the menu Build/Generate Signed APK
2. mv activity-reminder-release.apk apk/activity-reminder-1.x.y-RCz-release.apk

Manual Test
===========
1. Uninstall previous version
    adb -d uninstall com.beactiveapp
2. Install new version
    adb -d install -r apk/activity-reminder-1.x.y-RCz-release.apk
3. Open app from launcher & check version number
4. Go through Quick Start
4. Open app from notification
5. Disactivate reminders
    Check notification
6. Activate reminders
    Check notification
7. Open all the Help and Feedback options
    Check titles and messages
8. Share via Facebook
9. Share via Twitter
10. Share via Google+
11. Rate app
12. Reboot phone
    Check that BeActive is running
    Check output from pidcat
13. Turn OFF reminders & wait for inactivity limit
    Check that phone does NOT vibrate nor make a sound
14. Turn ON reminders
    Check that phone makes sound + 3 SHORT vibrations
15. Walk around with phone
    Check that phone makes sound + 2 LONG vibrations

Publish APK in Play Store
=========================
1. Publish as Staged rollout 1%, 10%, etc.

Create screenshots
==================

Update Store description
========================
* Update recent-changes and descriptions

Check that new version is active in Play Store
==============================================

Inform the world about the new version
======================================
* PR contacts spreadsheet
* etc.

Commit Release Changes
======================
    git status
    * git add ...
    * git commit -m "Release x.y.z"
    git push
    * git tag -a x.y.z -m 'x.y.z'
    git push --tags

