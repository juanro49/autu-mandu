# Car Report

## Description

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

## Get it

[Car Report on Play Store](https://play.google.com/store/apps/details?id=me.kuehle.carreport)
*(This is the full version.)*

[Car Report on F-Droid](https://f-droid.org/repository/browse/?fdid=me.kuehle.carreport)
*(This is a special FOSS version without Google Drive sync.)*

## Build it

The app uses gradle, so to build it just open a command line, switch to the app directory and
execute one of the following commands.

```
# Full version
gradle assembleFullRelease

# FOSS version
gradle assembleFossRelease
```


## Used libraries

* [Joda Time](http://joda-time.sourceforge.net)
* [HoloColorPicker](https://github.com/LarsWerkman/HoloColorPicker)
* [FloatingActionButton](https://github.com/makovkastar/FloatingActionButton)
* [Jackrabbit WebDAV](http://jackrabbit.apache.org)