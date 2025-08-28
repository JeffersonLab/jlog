# jlog [![CI](https://github.com/JeffersonLab/jlog/actions/workflows/ci.yml/badge.svg)](https://github.com/JeffersonLab/jlog/actions/workflows/ci.yml) [![Maven Central](https://badgen.net/maven/v/maven-central/org.jlab/jlog)](https://repo1.maven.org/maven2/org/jlab/jlog/)
The Jefferson Lab Java Logbook API for programmatic access to the [logbook server](https://logbooks.jlab.org/).

---
   - [Install](https://github.com/JeffersonLab/jlog#install)
   - [API](https://github.com/JeffersonLab/jlog#api)
   - [Example](https://github.com/JeffersonLab/jlog#example)
   - [Configure](https://github.com/JeffersonLab/jlog#configure)
   - [Build](https://github.com/JeffersonLab/jlog#build)
   - [Release](https://github.com/JeffersonLab/jlog#release)
   - [See Also](https://github.com/JeffersonLab/jlog#see-also)
---

## Install
This library requires a Java 11+ JVM and standard library at run time. 

You can obtain the library jar file from the [Maven Central repository](https://repo1.maven.org/maven2/org/jlab/jlog/) or from a Maven friendly build tool with the following coordinates (Gradle example shown):
```
implementation 'org.jlab:jlog:<version>'
```
Check the [Release Notes](https://github.com/JeffersonLab/jlog/releases) to see what has changed in each version.  

## API
[Javadocs](https://jeffersonlab.github.io/jlog/)

## Example
```
import org.jlab.jlog.LogEntry;
import org.jlab.jlog.exception.LogException;

public class HelloWorldDemo {
    public static void main(String[] args) throws LogException {
        LogEntry entry = new LogEntry("Hello World", "TLOG");
        
        long lognumber = entry.submitNow();
        
        System.out.println("Successfully submitted log entry number: " + lognumber);
    }
}
```

## Configure

### Properties
The default configuration properties are located in [jlog-default.properties](https://github.com/JeffersonLab/jlog/blob/master/src/main/resources/jlog-default.properties).   You can override them by creating your own jlog.properties and including it in your home directory.   Or you can programmatically set properties within a Java application using [Library.setConfiguration()](https://jeffersonlab.github.io/jlog/v5.1.0/org/jlab/jlog/Library.html#setConfiguration(java.util.Properties)).

### Authentication
In order to interact with the logbook server users must authenticate.  This is done using a [logbook server client certificate](https://logbooks.jlab.org/content/api-authentication), which is assumed to be located in the user's home directory in a file named _.elogcert_.  You can override the location of the certificate with [LogEntry.setClientCertificatePath()](https://jeffersonlab.github.io/jlog/v5.1.0/org/jlab/jlog/LogEntry.html#setClientCertificatePath(java.lang.String,boolean)).

## Build
This project is built with [Java 21](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 9](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/jlog
cd jlog
gradlew build
```

**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Release
1. Bump the version number in the VERSION file and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
2. The [CD](https://github.com/JeffersonLab/jlog/blob/main/.github/workflows/cd.yaml) GitHub Action should run automatically invoking:
   - The [Create release](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/gh-release.yaml) GitHub Action to tag the source and create release notes summarizing any pull requests.   Edit the release notes to add any missing details.
   - The [Publish artifact](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/maven-publish.yaml) GitHub Action to create a deployment artifact on maven central.
   - The [Publish docs](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/gh-pages-publish.yaml) GitHub Action to create javadocs.

## See Also
   - [logentrycli](https://github.com/JeffersonLab/logentrycli)
   - [Troubleshooting Tips](https://github.com/JeffersonLab/jlog/wiki/Troubleshooting)
