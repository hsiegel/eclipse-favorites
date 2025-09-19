# com.holgersiegel.favorites

Eclipse plug-in bundle providing the Favorites View. Built with Java 17 and Tycho; the bundle contributes the view, commands, handlers, and supporting model/persistence code.

## Contents
- `plugin.xml` – view/command/menu registrations
- `META-INF/MANIFEST.MF` – OSGi metadata (lazy activation, JavaSE-17)
- `src/` – plug-in implementation (model, handlers, view, DnD, persistence)
- `build.properties` – PDE build configuration

## Build
This bundle is built via the parent Tycho reactor. To compile it individually for debugging, run `mvn -pl com.holgersiegel.favorites eclipse:clean package` from the repository root.

## License
MIT License © 2025 Holger Siegel
