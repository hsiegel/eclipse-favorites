# Favorites View

Eclipse plug-in for a drag-and-drop favorites tree (files/folders) in the workbench.

## Install

Published update site:

- `https://hsiegel.github.io/eclipse-favorites/`

This keeps the same public p2 location as the earlier published site so existing
Eclipse installations can discover updates there.

### Requirements
- Java 17 or newer
- Maven 3.8.6 or newer
- Eclipse IDE

### Build
From the repository root:

```bash
mvn -V -U -B clean verify
```

This creates:

- `com.holgersiegel.favorites/target/com.holgersiegel.favorites_*.jar`
- `com.holgersiegel.favorites.updatesite/target/repository/`

### Troubleshooting Build
- Verify your runtime with `mvn -v` (Maven 3.8.6+ and Java 17+).
- If dependency resolution is flaky, clear Tycho artifacts and retry:
  `rm -rf ~/.m2/repository/org/eclipse/tycho && mvn -V -U -B clean verify`.

### Install in Eclipse (Update Site)
1. Open `Help -> Install New Software...`
2. Add the site `https://hsiegel.github.io/eclipse-favorites/`
3. Install `Favorites`
4. Restart Eclipse

### Publish an Updated Site to GitHub Pages
1. Build the repository: `mvn -V -B clean verify`
2. Replace the contents of `docs/` with `com.holgersiegel.favorites.updatesite/target/repository/`
3. Commit the updated `docs/` artifacts and push them to the GitHub repository that serves Pages

As long as GitHub Pages still serves this repository from `docs/` on the default
branch, the published update site URL remains unchanged.

### Local Install (Dropins)
1. Close Eclipse.
2. Copy `com.holgersiegel.favorites/target/com.holgersiegel.favorites_*.jar` to your Eclipse `dropins/` folder.
3. Start Eclipse with `-clean` once.
4. Open the view: `Window -> Show View -> Other... -> General -> Favorites`.

### Remove/Update
- Remove: delete the plug-in JAR from `dropins/` and restart Eclipse with `-clean`.
- Update: replace the JAR in `dropins/` with a freshly built one and restart with `-clean`.

## License
MIT License. See [LICENSE.md](./LICENSE.md).
