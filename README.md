# Mammut
Mammut is a multi-account capable Mastodon client for Android. It's built with offline support from the get-go for home and local timelines, and will soon allow for offline toot queuing. The key capabilities so far are:
* Multi-account support, including more than one account per instance
* Offline support
* Full theming capabilities, including 2 light and 2 dark themes
* Full support for custom emoji, including adding them when composing a toot

## App Structure
### Modules
Mammut is structured into a number of modules. This allows good separation of concerns, and faster build times.

* `app` - the main app module for Mammut. This consumes all other modules.
* `base` - the base module for feature modules. This includes all basic required dependencies for features, as well as a set of base classes for features to extend from.
* `data` - this module contains all data-related classes for the app, including database definitions, as well as repository implementations.
* `instances` - a simple wrapper library for the [instances.social](https://instances.social) REST API
* `notifications` - a feature module for the notifications feature
* `toot` - a feature module for toot composition

### App Architecture
Mammut is built with a number of different architecture considerations in mind:
* [The Repository Pattern](https://docs.microsoft.com/en-us/previous-versions/msp-n-p/ff649690(v=pandp.10)) - this allows abstraction of the source of data from other components in the app. This pattern is especially useful when trying to implement offline support.
* MVVM - Standard MVVM as recommended by Google is used throughout
* [Conductor](https://github.com/bluelinelabs/Conductor) - a replacement for Android Fragments that allows easier navigation, as well as a much nicer transitions framework.

## Building from source
Here's the steps to build yourself a debug variant of Mammut. 
* Get yourself an API token from the [instances.social token portal](https://instances.social/api/token). Remember, this is meant to be secret! Don't commit this to source control. 
* In [keys.gradle](./keys.gradle), replace the help message with your token. Be careful to not remove any `"`s OR `\`s. It should look something like this:
```
ext.INSTANCES_SECRET = "\"GRERU43534J..324\""
```
* Open up a terminal and navigate to where you've cloned the project. Run this command:
```
./gradlew installDebug
```

That's it! Look through your installed apps for Mammut, with a white icon.

## Disclaimer
This project is under heavy development. Things will probably change substantially between updates - apologies in advance. You can find it in beta on the Play Store [here](https://play.google.com/store/apps/details?id=io.github.koss.mammut).
