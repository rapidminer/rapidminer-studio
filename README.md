RapidMiner Studio Core
=============================

Easy-to-use visual environment for predictive analytics. No programming required. RapidMiner is easily the most powerful and intuitive graphical user interface for the design of analysis processes. Forget sifting through code! You can also choose to run in batch mode. Whatever you prefer, RapidMiner has it all.

This project contains the open source core of [RapidMiner Studio](https://rapidminer.com/studio).

## Getting Started

* [Install](https://rapidminer.com/products/studio/) RapidMiner Studio
* Have a look at our [Getting Started Central](https://rapidminer.com/getting-started-central/)
* You miss something? There might be an [Extension](https://marketplace.rapidminer.com) for it
* Have questions? Check out our official [community](https://community.rapidminer.com) and [documentation](https://docs.rapidminer.com)

## RapidMiner Studio Core as Dependency

Using Gradle:
```gradle
apply plugin: 'java'

repositories {
    maven { url 'https://maven.rapidminer.com/content/groups/public/' }
}

dependencies {
    compile group: 'com.rapidminer.studio', name: 'rapidminer-studio-core', version: '+'
}
```
Using Maven:
```xml
<project>
...
<repositories>
  <repository>
    <id>rapidminer</id>
    <url>https://maven.rapidminer.com/content/groups/public/</url>
  </repository>
</repositories>
...
<dependency>
  <groupId>com.rapidminer.studio</groupId>
  <artifactId>rapidminer-studio-core</artifactId>
  <version>LATEST</version>
</dependency>
...
</project>
```

## Build RapidMiner Studio Core from Source
1. Clone rapidminer-studio using [git](https://git-scm.com/) into a folder named `rapidminer-studio-core`
2. Execute `gradlew jar`
3. The jar file is located in __build/libs__

Please have in mind that the jar file still require all dependencies listed in the [build.gradle](build.gradle) file.

## Import RapidMiner Studio Core into your IDE
1. Your IDE has to support Gradle projects.
	1. Install [Gradle 2.3+](https://gradle.org/gradle-download/)
	2. Install and configure a Gradle plugin for your IDE
2. Import rapidminer-studio-core as a Gradle project

### Start the RapidMiner Studio Core GUI

To start the graphical user interface of RapidMiner Studio Core create a new `GuiLauncher.java` file in __src/main/java__ and run it with your IDE. If you want to use the generated jar, add the jar and all dependencies to the Java class path `java -cp "all;required;jars" GuiLauncher`. You can list the runtime dependencies by executing `gradlew dependencies --configuration runtime`.

```java
import com.rapidminer.gui.RapidMinerGUI;

class GuiLauncher {
	public static void main(String args[]) throws Exception {
		System.setProperty(PlatformUtilities.PROPERTY_RAPIDMINER_HOME, Paths.get("").toAbsolutePath().toString());
		RapidMinerGUI.registerStartupListener(new ToolbarGUIStartupListener());
		RapidMinerGUI.main(args);
	}
}
```

### Run RapidMiner Studio Core in CLI mode

**Prerequisite**: Start the RapidMiner Studio GUI at least once and accept the EULA.

To run RapidMiner Studio in command line mode create a new `CliLauncher.java` file in __src/main/java__ with the following content:

```java
import com.rapidminer.RapidMiner;

class CliLauncher {
	public static void main(String args[]) throws Exception {
		System.setProperty(PlatformUtilities.PROPERTY_RAPIDMINER_HOME, Paths.get("").toAbsolutePath().toString());
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
		RapidMiner.init();
	}
}
```

## Diving in

* Create your own [Extension](https://docs.rapidminer.com/latest/developers/creating-your-own-extension/)
* [Integrate](https://community.rapidminer.com/t5/Become-a-RapidMiner-Developer/Frequently-Asked-Questions-Development/m-p/19782) RapidMiner Studio Core into your project
* And much more at our [Developer Board](https://community.rapidminer.com/t5/Become-a-RapidMiner-Developer/bd-p/BARDDBoard)

## License

See the [LICENSE](LICENSE) file.