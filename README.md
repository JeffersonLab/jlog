# jlog [![Build Status](https://travis-ci.com/JeffersonLab/jlog.svg?branch=master)](https://travis-ci.com/JeffersonLab/jlog)
The Jefferson Lab Java Logbook API for programmatic access to the [logbook](https://logbooks.jlab.org/).

---
   - [Build](https://github.com/JeffersonLab/jlog#build)
   - [Configure](https://github.com/JeffersonLab/jlog#configure)
   - [Usage](https://github.com/JeffersonLab/jlog#usage)
   - [See Also](https://github.com/JeffersonLab/jlog#see-also)
---

## Build
```
gradlew build
```
**Note:** You can skip the automated tests with extra argument __-x test__
## Configure

### Properties
The default configuration properties are located in [elog.properties](https://github.com/JeffersonLab/jlog/blob/master/src/main/resources/elog.properties).   You can override them by creating your own elog.properties and including it in the classpath before the elog.jar file (containing the defaults).   Or you can programmatically set properties within a Java application using [Library.setConfiguration()](https://jeffersonlab.github.io/jlog/org/jlab/elog/Library.html#setConfiguration(java.util.Properties)).

### Authentication
In order to interact with the logbook server users must authenticate.  This is done using a [logbook server client certificate](https://logbooks.jlab.org/content/api-authentication), which is assumed to be located in the user's home directory in a file named _.elogcert_.  You can override the location of the certificate with [LogEntry.setClientCertificatePath()](https://jeffersonlab.github.io/jlog/org/jlab/elog/LogEntry.html#setClientCertificatePath(java.lang.String,boolean)).

## Usage
You can [download](https://github.com/JeffersonLab/jlog/releases) the library as a single jar file (there are no dependencies other than the JVM standard library), or reference the artifact in the [JCenter repository](https://dl.bintray.com/slominskir/maven) from a Gradle/Maven/Ivy project:
```
implementation 'org.jlab:jlog:4.0.1'
```

### API
   - [Javadocs](https://jeffersonlab.github.io/jlog/)
### Example
```
import org.jlab.elog.LogEntry;
import org.jlab.elog.exception.LogException;

public class HelloWorldDemo {
    public static void main(String[] args) throws LogException {
        LogEntry entry = new LogEntry("Hello World", "TLOG");
        
        long lognumber = entry.submitNow();
        
        System.out.println("Successfully submitted log entry number: " + lognumber);
    }
}
```

## See Also
   - [Old Release Notes](https://jeffersonlab.github.io/jlog/release-notes.html)
   - [Troubleshooting Tips](https://github.com/JeffersonLab/jlog/wiki/Troubleshooting)
