# Unreal 4 Project Copy / Rename Tool - aka "kopiarka"


This is a small utility that allows you to take one Unreal 4 Blueprint / C++ project and copy/rename/update file references from one directory to another.

It can also be used to move/rename any other directories or files but it may not behave 100% due to how it renames contents of ascii files.

The primary motivation for this tool was to be able to quickly work on one project to create a baseline, copy the baseline to a new project, and keep iterating without having to create a ton of git branches in a single project.

### Changelog
[Release 1.0.2 - Latest Download](https://github.com/jollymonsterstudio/ue4-kopiarka/releases/tag/1.0.2)
* fixed one issue where similar names as part of nested directories caused the old project name to be retained

Release 1.0.1
* fixed one minor issue where folders that had prefix or suffixes around the old project name would not get copied over correctly

Release 1.0.0
* initial project release
* only tested on Windows

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

Install Java 1.8 or greater
 
Instructions: https://www.java.com/en/download/help/index_installing.xml?os=Windows+10&j=8&n=20

### Configuration
The project comes with an `application.properties` that needs to be updated prior to execution and placed along side the jar file in the same directory you will be running it from.

Sample properties

```properties
# ------------------
# REQUIRED PARAMS
# ------------------

# no trailing slashes
project.source.directory=c:\\OldProject
# NOTE: project names should be as unique as possible due to the way this tool manipulates strings.
#       for example if your project is called UAnim any references to your classes / code / configs that contain that term will be replaced ... regardless if you needed it not to be replaced
project.source.name=OldProject

# no trailing slashes
project.target.directory=c:\\NewProject
# NOTE: project names should be as unique as possible due to the way this tool manipulates strings.
#       for example if your project is called UAnim any references to your classes / code / configs that contain that term will be replaced ... regardless if you needed it not to be replaced
project.target.name=NewProject

# directories to include in the copy, specifically excluding any auto generated ones. comma separated.
whitelist.directories=Config,Content,Source

# extensions of binary files you want to copy. binary files are not examined internally for old project references. comma separated.
whitelist.extension.binary=uasset,umap,png,jpg,jpeg,wav

# extensions of ascii files you want to copy. ascii files are examined and modified during the copy with new project names. comma separated.
whitelist.extension.ascii=ini,cpp,h,uproject,sln,cs,gitignore,md,txt

# ------------------
# OPTIONAL PARAMS
# ------------------
# this parameter tries to delete the target directory before copying
# if parameter is omitted defaults to false
config.force.delete=false
```


### Installing and Running

1. Download the latest release ( zip )
1. Extract zip file to a location on your hard drive
1. Update `application.properties` with your folder / project details
1. Run the application using `java -jar ue4-kopiarka.jar`

Sample console output

```
C:\projects\kopiarka\target>java -jar ue4-kopiarka.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.5.RELEASE)

11:54:47.450 [main] INFO  c.j.u.UnrealProjectCopyApplication - Starting UnrealProjectCopyApplication on UNICRON with PID 19672 (C:\projects\kopiarka\target\ue4-kopiarka.jar started by zinczukw in C:\projects\kopiarka\target)
11:54:47.455 [main] INFO  c.j.u.UnrealProjectCopyApplication - No active profile set, falling back to default profiles: default
11:54:47.523 [main] INFO  o.s.c.a.AnnotationConfigApplicationContext - Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@510f3d34: startup date [Wed Nov 28 11:54:47 EST 2018]; root of context hierarchy
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.springframework.cglib.core.ReflectUtils$1 (jar:file:/C:/projects/kopiarka/target/ue4-kopiarka.jar!/BOOT-INF/lib/spring-core-5.0.9.RELEASE.jar!/) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of org.springframework.cglib.core.ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
11:54:48.023 [main] INFO  c.j.unreal.service.CopyService - ------------
11:54:48.023 [main] INFO  c.j.unreal.service.CopyService -   STARTED
11:54:48.025 [main] INFO  c.j.unreal.service.CopyService - ------------
11:54:48.033 [main] INFO  c.j.unreal.service.CopyService - FORCE DELETE ENABLED - Trying to delete: C:\UE4Projects\PunchKick03Testing
11:54:48.430 [main] INFO  c.j.unreal.service.CopyService - Processing dir: C:\UE4Projects\PunchKick02
11:54:48.596 [main] INFO  c.j.unreal.service.CopyService - Size of dir: 5 GB
11:54:48.596 [main] INFO  c.j.unreal.service.CopyService - Copying contents to dir: C:\UE4Projects\PunchKick03Testing
11:54:48.599 [main] INFO  c.j.unreal.service.CopyService - This can take a bit depending on the size of your project .... DO NOT PANIC ... and if you do just delete the target directory and start again !
File Copy Progress:  100% [===================] 1352/1352 (0:00:01 / 0:00:00)
11:54:49.806 [main] INFO  c.j.unreal.service.CopyService - WHEW ! we made it
11:54:49.807 [main] INFO  c.j.unreal.service.CopyService - New dir location: C:\UE4Projects\PunchKick03Testing
11:54:49.958 [main] INFO  c.j.unreal.service.CopyService - Size of new dir: 5 GB
11:54:49.959 [main] INFO  c.j.unreal.service.CopyService - ------------
11:54:49.960 [main] INFO  c.j.unreal.service.CopyService -   FINISHED
11:54:49.960 [main] INFO  c.j.unreal.service.CopyService - ------------
11:54:49.963 [main] INFO  o.s.c.a.AnnotationConfigApplicationContext - Closing org.springframework.context.annotation.AnnotationConfigApplicationContext@510f3d34: startup date [Wed Nov 28 11:54:47 EST 2018]; root of context hierarchy

```

## How to build

This is a [Maven project](https://maven.apache.org/) so you can simply run the following
```
mvn clean package
```
Which will produce a `ue4-kopiarka.jar` artifact in the `[project dir]/target` directory

The jar file will require `application.properties` to be in the same directory when running.

## Compatibility 
This application was tested on the following operating systems:
```
Windows 7   - JDK 1.8
Windows 10  - JDK 1.8
```
Should be compatible with macOSX as well as Linux ( may require tweaks to the path validation )

## Testing
Currently tests are disabled as they simply trigger the copy function against an example project structure. 

If additional functionality is required this section will be expanded.

## Built With

* [Spring Boot](http://spring.io/projects/spring-boot) - The primary framework
* [Apache Commons](https://commons.apache.org/) - Various libraries for handling common operations
* [tongfei-progressbar](https://github.com/ctongfei/progressbar) - Progress bar

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Authors

* **Wojtek Zinczuk** - *Initial work* - [Jolly Monster Studio](http://jollymonsterstudio.com)

See also the list of [contributors](https://github.com/jollymonsterstudio/ue4-kopiarka/graphs/contributors) who participated in this project.

## License
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This project is licensed under the Apache 2.0 - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

Source material
* https://answers.unrealengine.com/questions/45690/how-i-can-rename-my-pojects.html
* https://answers.unrealengine.com/questions/242407/renaming-a-c-project.html
* https://www.youtube.com/watch?v=cmK-A8Dh_Ms
* https://forums.unrealengine.com/development-discussion/blueprint-visual-scripting/24493-migrate-code-based-blueprint
