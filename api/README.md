# Heavy Inventories API

Public integration contracts for Minecraft 26.3: item and inventory weights, capacity providers, HUD hooks, and
lifecycle notifications. Plugin registration is supported on Fabric and NeoForge.

The [developer wiki](../wiki/Developers.md) covers setup, registration, and usage.

## Dependency

- Repository: `https://maven.iso2t.com/releases`
- Coordinates: `com.iso2t.heavyinventories:api:<api_version>`
- Version: `api_version` in the root `gradle.properties`
- Java: 25

Use the API as a compile-only dependency alongside the matching Minecraft development environment. HI's loader jars
already contain these classes; do not bundle a second copy. The API does not depend on HI's implementation, loader
APIs, or EasyConfig.

The module produces the API jar, sources, and Javadocs. See [Getting started](../wiki/API-Getting-started.md) for the
current coordinates and dependency setup.
