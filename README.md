# ETF Test Driver for testing XML files with BaseX

The test driver is loaded by the ETF framework at runtime. The test driver
loads the test data into the integrated [BaseX database](http://basex.org/) and
executes XQuery data test scripts bundled as ETF project files.

[![European Union Public Licence 1.2](https://img.shields.io/badge/license-EUPL%201.2-blue.svg)](https://joinup.ec.europa.eu/software/page/eupl)
[![Latest version](http://img.shields.io/badge/latest%20version-3.0.0-blue.svg)](https://github.com/etf-validator/etf-bsxtd/releases)
[![Java CI](https://github.com/etf-validator/etf-bsxtd/actions/workflows/gradle.yml/badge.svg)](https://github.com/etf-validator/etf-bsxtd/actions/workflows/gradle.yml)

&copy; 2017-2022 European Union, interactive instruments GmbH. Licensed under the EUPL.

## About ETF

ETF is an open source testing framework for validating spatial data, metadata and web services in Spatial Data Infrastructures (SDIs). For documentation about ETF, see [http://docs.etf-validator.net](http://docs.etf-validator.net/).

Please report issues [in the GitHub issue tracker of the ETF Web Application](https://github.com/interactive-instruments/etf-webapp/issues).

ETF component version numbers comply with the [Semantic Versioning Specification 2.0.0](http://semver.org/spec/v2.0.0.html).

## Build information

The project can be build and installed by running the gradlew.sh/.bat wrapper with:
```gradle
$ gradlew build install
```

## Installation
Copy the JAR path to the _$driver_ directory. The $driver directory is configured in your _etf-config.properties_ configuration path as variable _etf.testdrivers.dir_. If the driver is loaded correctly, it is displayed on the status page.

## Updating
Remove the old JAR path from the _$driver_ directory and exchange it with the new version.

## Developing BaseX-based Executable Test Suites

[The ETF developer manual](http://docs.etf-validator.net/Developer_manuals/Developing_Executable_Test_Suites.html) describes how to develop Executable Test Suites using BaseX.
