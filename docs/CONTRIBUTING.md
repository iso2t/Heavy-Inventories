# Contributing to Heavy Inventories

Use JDK 25 and the included Gradle 9.5.0 wrapper:

```sh
# Linux/macOS
bash ./gradlew clean build

# Windows PowerShell
.\gradlew.bat clean build
```

The build runs common regression tests and checks metadata, mixins, services, enchantment resources, bundled weight definitions, and test-harness exclusion in both loader jars. Distributable jars are under `fabric/build/libs/` and `neoforge/build/libs/`; do not install sources, javadoc, or lifecycle-test jars.

The GitHub Actions workflow builds/tests on Linux and Windows and uploads reports and mod artifacts. It does not publish releases or start Minecraft. The [testing guide](TESTING.md) describes opt-in local runtime checks and their evidence.

Source and issue tracking: [iso2t/Heavy-Inventories](https://github.com/iso2t/Heavy-Inventories). Contributions and translations are welcome. Licensed under [MIT](../LICENSE.md).
