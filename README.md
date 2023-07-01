# Autu Mandu

---


> Android app for saving and looking up costs of your car.

Autu Mandu is an android app, based on descontinued [Car Report](https://bitbucket.org/frigus02/car-report) which lets you enter refuelings and other income and expenses of your cars and displays nice reports. The following are currently included:

1. Fuel consumption
1. Fuel price
1. Station of refueling
1. Mileage
1. Costs in general

You can add reminders based on mileage and time for car related recurring actions, e.g. general
inspection once a year.

It provides synchronization with webdav and has basic backup/restore and CSV
import/export functionality.

## Install

[Autu Mandu on F-Droid](https://f-droid.org/repository/browse/?fdid=org.juanro.autumandu)

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

[Apache 2.0 © Juanro49.](../COPYING)
