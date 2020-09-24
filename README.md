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
## Configure
The default configuration properties are located in elog.properties.   You can override them by creating your own elog.properties and including it in the classpath before the elog.jar file (containing the defaults).   Or you can programmatically set properties within a Java application using Library.setConfiguration().

## Usage
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
   - [Download](https://github.com/JeffersonLab/jlog/releases)
   - [Javadocs](https://jeffersonlab.github.io/jlog/)
   - [Old Release Notes](https://jeffersonlab.github.io/jlog/release-notes.html)
   - [Troubleshooting Tips](https://github.com/JeffersonLab/jlog/wiki/Troubleshooting)
