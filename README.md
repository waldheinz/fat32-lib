fat32-lib
=========

[![Flattr fat32-lib](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=waldheinz&url=https://github.com/waldheinz/fat32-lib&title=fat32-lib&language=en&tags=github&category=software)

This library allows to manipulate FAT file systems using the Java programming language.
Because of it's age and simplicity, FAT can be called the least common denominator in
file systems, being used in digital cameras, cell phones, ... and being supported by
almost every operating system in existence. This project aims for making FAT file
systems accessible for Java programs without using the operating system to interpret
the on-disk structures. Instead, we provide a pure - Java implementation of the FAT
specification from MICROS~1. 

Features
--------

The following features are currently supported:

  * creating FAT12, FAT16 and FAT32 file systems through the super floppy formatter
  * r/w access to FAT12, FAT16 and FAT32 file systems
  * manipulating the FAT file attributes (archive, hidden, system and read-only)
  * r/w access to the FAT's volume label
  * no external dependencies

Getting started
---------------

To use the fat32-lib you will have to add it to the classpath of your project. For Maven
users it is sufficient to add

~~~~
<dependency>
    <groupId>de.waldheinz</groupId>
    <artifactId>fat32-lib</artifactId>
    <version>0.6.5</version>
</dependency>
~~~~

to the dependencies section of your pom.

History
-------

This library was originally based on the FAT file system driver included in the JNode operating
system. Since then, many bugs were fixed, the code was re-factored several times, and now I think
it is fair to call the fat32-lib a unique implementation of the FAT file system family.
