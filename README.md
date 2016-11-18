# RdsDataDownloader

Downloads data from RDS and stores it to disk.

* [Installation](#installation)
* [Configuration Settings](#configuration-settings)
* [Questions/Support](#questionssupport)

## Installation

* Get Java 8
* Create a directory to run `RdsDataDownloader` in. E.g.: `mkdir rds-data-downloader`
* `cd` into that directory. E.g.: `cd rds-data-downloader`
* Put the sample config file from
  [sample-conf/application.properties](https://github.com/seleritycorp/RdsDataDownloader/tree/master/sample-conf/application.properties)
  to `conf/application.properties`.
* Change `YOUR_USER` to your user name
* Change `YOUR_PASSWORD` to your password
* Get the `jar` of `RdsDataDownloader` that you want to run (either by cloning this repo and running `mvn package`
  or fetch it from [Maven Central](https://repo1.maven.org/maven2/com/seleritycorp/rds/downloader/RdsDataDownloader)
  and store it into that directory.
* Run `java -jar RdsDataDownloader-1.0.0.jar` (update the version number accordingly)

`RdsDataDownloader` will download fresh RDS data and persist them to `data/rds/rds-data.json` every hour.
This file gets it's data atomically, so other processes can read from it all the time.

## Configuration Settings

* `CoreServices.url` The url to connect for Selerity's CoreServices. 
* `CoreServices.user` The user used for CoreService authentication.
* `CoreServices.password` The password used for CoreService authentication.
* `RdsDataDownloader.fetcher.enumTypes` The Reference data `enumTypes` to fetch data for. E.g.: `PUBLIC_COMPANY` for
  reference data for public companies.

## Questions/Support

If you run into issues or have questions, please let us know at support@seleritycorp.com
