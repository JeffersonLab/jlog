# jlog [![Java CI with Gradle](https://github.com/JeffersonLab/jlog/actions/workflows/gradle.yml/badge.svg)](https://github.com/JeffersonLab/jlog/actions/workflows/gradle.yml) [![Maven Central](https://img.shields.io/maven-central/v/org.jlab/jlog)](https://repo1.maven.org/maven2/org/jlab/jlog/)
The Jefferson Lab Java Logbook API for programmatic access to the [logbook server](https://logbooks.jlab.org/).

---
   - [Usage](https://github.com/JeffersonLab/jlog#usage)
     - [API](https://github.com/JeffersonLab/jlog#api) 
     - [Example](https://github.com/JeffersonLab/jlog#example) 
     - [Configure](https://github.com/JeffersonLab/jlog#configure) 
   - [Build](https://github.com/JeffersonLab/jlog#build)
   - [See Also](https://github.com/JeffersonLab/jlog#see-also)
---

## Usage
The library is a single jar file with no dependencies other than the Java 8+ JVM and standard library.  You can obtain the jar file from the [Maven Central repository](https://repo1.maven.org/maven2/org/jlab/jlog/) directly or from a Maven friendly build tool with the following coordinates (Gradle example shown):
```
implementation 'org.jlab:jlog:5.0.0'
```
You can check the [Release Notes](https://github.com/JeffersonLab/jlog/releases) to see what has changed in each version.  
### API
   - [Javadocs](https://jeffersonlab.github.io/jlog/)
### Example
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
### Configure
#### Properties
The default configuration properties are located in [jlog-default.properties](https://github.com/JeffersonLab/jlog/blob/master/src/main/resources/jlog-default.properties).   You can override them by creating your own jlog.properties and including it in your home directory.   Or you can programmatically set properties within a Java application using [Library.setConfiguration()](https://jeffersonlab.github.io/jlog/org/jlab/jlog/Library.html#setConfiguration(java.util.Properties)).

#### Authentication
In order to interact with the logbook server users must authenticate.  This is done using a [logbook server client certificate](https://logbooks.jlab.org/content/api-authentication), which is assumed to be located in the user's home directory in a file named _.elogcert_.  You can override the location of the certificate with [LogEntry.setClientCertificatePath()](https://jeffersonlab.github.io/jlog/org/jlab/jlog/LogEntry.html#setClientCertificatePath(java.lang.String,boolean)).

## Build
This [Java 8+](https://adoptopenjdk.net/) project uses the [Gradle 6](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/jlog
cd jlog
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## See Also
   - [Developer Notes](https://github.com/JeffersonLab/jlog/wiki/Developer-Notes)
   - [Troubleshooting Tips](https://github.com/JeffersonLab/jlog/wiki/Troubleshooting)
   - [Old Release Notes](https://jeffersonlab.github.io/jlog/release-notes.html)
