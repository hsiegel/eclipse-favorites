# Favorites View

Eclipse plug-in for a drag-and-drop favorites tree (files/folders) in the workbench.

## Local Build and Install

This repository intentionally contains no published update site.  
Users build the plug-in themselves from readable source and install it locally.

### Requirements
- Java 17 or newer
- Maven 3.8.6 or newer
- Eclipse IDE

### Build
From the repository root:

```bash
mvn -V -U -B clean verify
```

This creates the plug-in JAR in:

- `com.holgersiegel.favorites/target/com.holgersiegel.favorites_*.jar`

### Troubleshooting Build
- Verify your runtime with `mvn -v` (Maven 3.8.6+ and Java 17+).
- If dependency resolution is flaky, clear Tycho artifacts and retry:
  `rm -rf ~/.m2/repository/org/eclipse/tycho && mvn -V -U -B clean verify`.

### Install in Eclipse (Dropins)
1. Close Eclipse.
2. Copy `com.holgersiegel.favorites/target/com.holgersiegel.favorites_*.jar` to your Eclipse `dropins/` folder.
3. Start Eclipse with `-clean` once.
4. Open the view: `Window -> Show View -> Other... -> General -> Favorites`.

### Remove/Update
- Remove: delete the plug-in JAR from `dropins/` and restart Eclipse with `-clean`.
- Update: replace the JAR in `dropins/` with a freshly built one and restart with `-clean`.

## License
MIT License. See [LICENSE.md](./LICENSE.md).
