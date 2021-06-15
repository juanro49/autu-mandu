# Car Report

---

**Not maintained:** Car Report has been a project I created and maintained in my free time. I realized that I don't enjoy maintaining it anymore and so I decided to archive it. The app is now unpublished on Google Play. Feel free to keep using the app as long as it works. I hope it has been useful to you. :-)

---

> Android app for saving and looking up costs (refuelings, ...) of your car.

Car Report is an android app, which lets you enter refuelings and other income and expenses of your
cars and displays nice reports. The following are currently included:

1. Fuel consumption
1. Fuel price
1. Mileage
1. Costs in general

You can add reminders based on mileage and time for car related recurring actions, e.g. general
inspection once a year.

It provides synchronization with Dropbox and Google Drive and has basic backup/restore and CSV
import/export functionality.

## Install

[Car Report on Play Store](https://play.google.com/store/apps/details?id=me.kuehle.carreport)
_(This is the full version.)_

[Car Report on F-Droid](https://f-droid.org/repository/browse/?fdid=me.kuehle.carreport)
_(This is a special FOSS version without Google Drive sync.)_

## Build

The app uses gradle, so to build it just open a command line, switch to the app directory and
execute one of the following commands.

```
# Full version
gradle assembleFullRelease

# FOSS version
gradle assembleFossRelease
```

**Note:** It seems gradle will try to download the Google Play Services libraries in a FOSS build,
although they are not used for compiling. If you don't have these libraries available, you need
to temporary remove all lines prefixed with `fullCompile` from the _build.gradle_ file in the
app folder. See [this comment](https://bitbucket.org/frigus02/car-report/issues/53/dependence-on-proprietary-components#comment-21959839)
for more information.

## License

[Apache 2.0 © Jan Kühle.](../COPYING)
