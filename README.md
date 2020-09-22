# BeeGenUtilitiesWindows

[![N|Solid](jgernnotes200x45.png)](http://www.jgen.eu/?p=900&preview=true)

Overview
========


There are two command-line utilities assisting developers in extracting information from the CA Gen Local Model.  They are both 32-bit Java command-line programs running on Windows and using the CA Gen JMMI API to access local models.

Summary of utilities:

- BeeGenExtractorJSON
This utility accesses CA Gen Local Model and extracts metadata from the model and exporting the entire model's contents to two JSON text files.
- BeeGenExtractorSQLite
This utility accesses the CA Gen Local Model and extracts its contents creating and populating an SQLite database.


[Bee Gen API for Swift doumentation can be downloded here.](https://github.com/JGen-Notes/BeeGenAPIJava/blob/master/eu.jgen.beegen.model.api/BeeGenAPIDoc.zip)

More about project you can find [here](http://www.jgen.eu/?p=900&preview=true).

> The Bee Gen Model Framework is still under
> development and subject to changes.
> 

Versions of used Software
=========================

- [SQLite Release 3.33.0 On 2020-08-14](https://sqlite.org/index.html)

- [sqlite-jdbc-3.32.3.2](https://github.com/xerial/sqlite-jdbc/releases)

- [Java SE 8 1.8.0_05](https://www.oracle.com/java/technologies/javase-jre8-downloads.html)

- [Eclipse Version: 2020-06 (4.16.0)](https://www.eclipse.org/downloads/)

You are using the wrong version of Java to run utilities in case you have the following exception message:

```sh
Exception in thread "main" java.lang.UnsatisfiedLinkError: C:\Program Files (x86)\CA\Gen86Free\Gen\genmodel_client.dll: Can't load IA 32-bit .dll on a AMD 64-bit platform
```

Example of use
==============

Sample use of BeeGenExtractorJSON.

```sh
Bee Gen  Model Extractor, Version 0.5, Schema Level 9.2.A6
Extracts meta data from the CA Gen Model and creates two JSON files showing entire model contents.
USAGE:
	pathModel      -   Location of the directory containing CA Gen Local Model (directory ending with .ief)
Connecting to the CA Gen Model in the directory 'C:\Gen\Models\beegen01.ief'
Connected to the model BEEGEN01...
Extracting object and property definitions...
Extracting associations definitions...
Two transaction files have been created in the sub-folder 'bee' of your CA Gen model 'BEEGEN01' at location 'C:\Gen\Models\beegen01.ief'
Run Statistics:
	Number of exported object definitions is 1228
	Number of exported property definitions  is 4174
	Number of exported association definitions is 3722
Transactions extraction completed.
```

It produces the following output:

```sh
List of action blocks in the model: BEEGEN01, Using schema level: BEEGEN01

	Action block name: PERSON_CREATE, having id: 22020096
	Action block name: PERSON_DELETE, having id: 22020097
	Action block name: PERSON_UPDATE, having id: 22020098
	Action block name: PERSON_READ, having id: 22020099
	Action block name: PERSON_LIST, having id: 22020100

Completed.
```
Sample JSON generated for the objects and properties.

```sh
[
  {
    "id" : 22020096,
    "type" : 21,
    "mnemonic" : "ACBLKBSD",
    "properties" : [
      {
        "type" : 30,
        "format" : "INT",
        "mnemonic" : "CEID",
        "value" : "1049"
      },
      {
        "type" : 232,
        "format" : "SINT",
        "mnemonic" : "OPCODE",
        "value" : "21"
      },
      {
        "type" : 224,
        "format" : "NAME",
        "mnemonic" : "NAME",
        "value" : "PERSON_CREATE"
      },
      {
        "type" : 216,
        "format" : "INT",
        "mnemonic" : "MODDATE",
        "value" : "20200831"
      },
      {
        "type" : 219,
        "format" : "INT",
        "mnemonic" : "MODTIME",
        "value" : "19545839"
      },

```
Sample JSON generated for associations.

```sh
[
  {
    "from" : 22020096,
    "card" : "1",
    "mnemonic" : "GRPBY",
    "type" : 88,
    "inverseType" : 381,
    "to" : 6291508,
    "seqno" : 0,
    "direction" : "F"
  },
  {
    "from" : 22020096,
    "card" : "M",
    "mnemonic" : "USESEXST",
    "type" : 611,
    "inverseType" : 659,
    "to" : 134217729,
    "seqno" : 0,
    "direction" : "F"
  },
  {
    "from" : 22020096,
    "card" : "M",
    "mnemonic" : "USESEXST",
    "type" : 611,
    "inverseType" : 659,
    "to" : 134217728,
    "seqno" : 1,
    "direction" : "F"
  },
```
The second utility requires SQLite to be installed before you can run it. It will create a database and populate with the data extracted from the CA Gen model.

Sample use of BeeGenExtractorSQLite

```sh
Bee Gen Model Creator, Version: 0.5,  Schema Level: 9.2.A6
Extracts meta data from the CA Gen Model and creates Bee Gen Model.
USAGE:
	pathModel      -   Location of the directory containing local CA Gen Model (directory name should end with .ief)

Connecting to the CA Gen Model in the directory 'C:\Gen\Models\beegen01.ief'
Connected to the model BEEGEN01...
Tables dropped...
Tables created...
Loading objects and properties...
Loading associations...
Loading meta data for objects...
Loading meta data for properties...
Loading meta data for associations...
Tables populated...
BeeGen Model 'BEEGEN01.db' has been created in the sub-folder 'bee' of your CA Gen model 'BEEGEN01' at location 'C:\Gen\Models\beegen01.ief'
Run Statistics:
	Number of exported objects is 1228
	Number of exported properties is 4174
	Number of exported associations is 3722
	Number of exported meta objects is 642
	Number of exported meta properties is 14433
	Number of exported meta associations is 9423
Model extraction completed.
```
