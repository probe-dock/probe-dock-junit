## v0.5.1 - March 8, 2016

* Fixed NPE when any exception is raised in @BeforeClass (probably same behavior with @AfterClass)

## v0.5.0 - March 8, 2016

* Updated the Java lib to v0.4.0 (brings SCM data support, see: https://github.com/probedock/probedock-java/blob/master/CHANGELOG.md for more details)
* Added the possibility to retrieve the file path and line number of each test.

## v0.4.1 - November 9, 2015

* Fixed: https://github.com/probedock/probedock-junit/issues/1
* Bumped version of Probe Dock lib

## v0.4.0 - October 28, 2015

* Bumped version of Probe Dock lib

## v0.3.0 - October 20, 2015

* `testIgnored` and `testAssumptionFailure` will set the test as `inactive`. In general, both event are supposed to be tests skipped by JUnit runner.

## v0.2.3 - October 19, 2015

* Changed `unit` category by `JUnit`. This will be always true with this probe. Remember, the category can be configured by project or annotations.

## v0.2.2 - August 31, 2015

* Bumped lib version

## v0.2.0 - June 09, 2015

* **Breaking changes**
* Bumped the Probe Dock Java lib to version v0.2.0 (see: [changelog](https://github.com/probedock/probedock-java/blob/master/CHANGELOG.md))
* Gather more information attached in the metadata of the test run like the memory consumption, OS info, ...

## v0.1.5 - June 02, 2015

* Added `:` in test names between humanize class name and humanize method name.

## v0.1.1 - v0.1.4 - May 28, 2015 - June 01, 2015

* Several bug fixes
* Added correct support of JUnit parameterized tests

## v0.1.0 - May 18, 2015

* Refactoring of the old Junit client.
