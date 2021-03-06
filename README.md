# RdsDataDownloader

Downloads data from RDS and stores it to disk.

* [Installation](#installation)
* [Configuration Settings](#configuration-settings)
* [JavaDoc](#javadoc)
* [Questions/Support](#questionssupport)

[![Build Status](https://travis-ci.org/SelerityInc/RdsDataDownloader.svg?branch=master)](https://travis-ci.org/SelerityInc/RdsDataDownloader)

## Installation

* Get Java 8
* Create a directory to run `RdsDataDownloader` in. E.g.: `mkdir rds-data-downloader`
* `cd` into that directory. E.g.: `cd rds-data-downloader`
* Put the sample config file from
  [sample-conf/application.properties](https://github.com/seleritycorp/RdsDataDownloader/tree/master/sample-conf/application.properties)
  to `conf/application.properties`, or if you prefer to put it in another location,
  then you need to pass it as command line argument using `--config <file>`.
* Change `YOUR_USER` to your user name
* Change `YOUR_PASSWORD` to your password
* Get the `jar` of `RdsDataDownloader` that you want to run (either by cloning this repo and running `mvn package`
  or fetch it from [Maven Central](https://repo1.maven.org/maven2/com/seleritycorp/rds/downloader/RdsDataDownloader))
  and store it into that directory.
* Run `java -Xmx256M -jar RdsDataDownloader-1.0.0.jar`, if your config file is in a custom location specify it with the option `--config <file>`
 (update the version  number accordingly).

`RdsDataDownloader` will download fresh RDS data and persist them to the configured `paths.data` folder every hour.
This file gets it's data atomically, so other processes can read from it all the time.

## Configuration Settings

* `paths.data` Folder where the data is stored. If not absolute, this path is relative to the working dir.
* `paths.dataState` Folder where the app state is stored. If not absolute, this path is relative to `paths.data` dir.
* `RdsDataDownloader.persister.target` File where fetched data is persisted.
  If not absolute, this file is relative to `paths.data` dir. Will be created if does not exists.
* `CoreServices.url` The url to connect for Selerity's CoreServices.
* `CoreServices.user` The user used for CoreService authentication.
* `CoreServices.password` The password used for CoreService authentication.
* `RdsDataDownloader.fetcher.enumTypes` The Reference data `enumTypes` to fetch data for. E.g.: `PUBLIC_COMPANY` for
  reference data for public companies.

## JavaDoc

JavaDoc for this package is available at https://doc.seleritycorp.com/javadoc/com.seleritycorp.rds.downloader/RdsDataDownloader/master/

JavaDoc for the whole Selerity platform is at https://doc.seleritycorp.com/javadoc/platform/master/

## Questions/Support

If you run into issues or have questions, please let us know at support@seleritycorp.com
