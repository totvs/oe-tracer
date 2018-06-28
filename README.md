# oe-tracer
OpenEdge Tracer - [Sonarqube](https://www.sonarqube.org/) Plug-in

This repository contains an extension of Sonarqube Coverage Plug-in for using on OpenEdge ABL projects. 
## Sonarqube Test Coverage
This plug-in uses a generic format for Sonarqube's test coverage and test execution import known as [Generic Test Data](https://docs.sonarqube.org/display/SONAR/Generic+Test+Data). The Sonarqube's coverage format is a simple XML and looks like this:
```
<coverage version="1">
  <file path="file1.p">
    <lineToCover lineNumber="6" covered="true"/>
    <lineToCover lineNumber="7" covered="false"/>
  </file>
  <file path="file2.p">
    <lineToCover lineNumber="3" covered="true"/>
  </file>
</coverage>
```
The root node should be named "coverage" and its version attribute should be set to "1". Insert a "file" element for each file which can be covered by tests. Its "path" attribute can be either absolute or relative to the root of the module.
Inside a "file" element, insert a "lineToCover" for each line which can be covered by unit tests. It can have the following attributes:
* "lineNumber" (mandatory): number of line with [executable statements](https://docs.sonarqube.org/display/DEV/Executable+Lines).
* "covered" (mandatory): boolean value indicating whether tests actually hit that line.
## OpenEdge Tracer
This plug-in parses ABL sources for identifying executable statements from the [oe-proparse parser](https://github.com/devtotvs/oe-proparse). This plug-in generates a Sonar's XML test data as output. 
Some statements are ignored because is not used to calculate missing test coverage for files that are not included in coverage report. Such ignored statements include:
* NodeTypes.USING
* NodeTypes.DEFINE
* NodeTypes.DO
* NodeTypes.PROCEDURE
* NodeTypes.PERIOD
* NodeTypes.REPEAT
* NodeTypes.METHOD
* NodeTypes.FUNCTION
* NodeTypes.CLASS
* NodeTypes.CONSTRUCTOR
## Running Plug-in
This oe-coverage plug-in is a CLI application. This application can be used with ant builders or maven tasks.
```
Tracer <base-dir> <source> <propath> <schema> <output.xml>
```
* "base-dir" (mandatory): project base directory.
* "source" (mandatory): source file or directory of ABL projects.
* "schema" (mandatory): database schema required for parsing ABL sources. Schema can be generated from OpenEdge Database using [this toolset](https://github.com/devtotvs/oe-proparse/blob/master/prorefactor/configdump/configdump.p).
* "sonar-output" (mandatory): Sonar's XML generic test
