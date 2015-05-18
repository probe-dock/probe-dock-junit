# Probe Dock Junit

**Junit client probe for [Probe Dock](https://github.com/probe-dock/probe-dock).**

[![License](https://img.shields.io/github/license/probe-dock/probe-dock-junit.svg)](LICENSE.txt)

## Requirements

* Java 6+

## Installation for Maven

In your pom file, add the following dependency:

```xml
<dependency>
  <groupId>io.probedock.client</groupId>
  <artifactId>probe-dock-junit</artifactId>
  <version>0.1.0</version>
</dependenc>
```

Configure your maven surefire plugin to add a Junit listener:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>2.16</version>
    <configuration>
        <properties>
            <property>
                <name>listener</name>
                <value>io.probedock.client.junit.ProbeListener</value>
            </property>
        </properties>
    </configuration>
</plugin>
```

## Contributing

* [Fork](https://help.github.com/articles/fork-a-repo)
* Create a topic branch - `git checkout -b my_feature`
* Push to your branch - `git push origin my_feature`
* Create a [pull request](http://help.github.com/pull-requests/) from your branch

Please add a [changelog](CHANGELOG.md) entry with your name for new features and bug fixes.

## License

Probe Dock Junit is licensed under the [MIT License](http://opensource.org/licenses/MIT).
See [LICENSE.txt](LICENSE.txt) for the full license.
