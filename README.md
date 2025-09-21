# Favorites View

## HOWTO
- **Build**: `mvn -V -U -B clean verify`
- **Install**: In Eclipse `Help -> Install New Software...` use the published update site `https://github.com/hsiegel/eclipse-favorites` or the local build at `com.holgersiegel.favorites.updatesite/target/repository`. *Fallback*: `File -> Export -> Deployable plug-ins and fragments` on the `com.holgersiegel.favorites` project into your dropins folder.
- **Use**: `Window ▸ Show View ▸ Other… ▸ General ▸ Favorites`, then drag resources or files into the view.

## Overview
The Favorites View plug-in provides a drag-and-drop enabled tree for quickly pinning frequently used files and folders. Entries persist across restarts, expose context menu commands (open, reveal, copy path, remove), support reordering, and integrate with the workbench via a toolbar and a key binding (`Ctrl+Alt+F` / `Cmd+Alt+F`) to add the active editor.

## Key Features
- Drag & drop from Package/Project Explorer, navigator views, and the OS (FileTransfer, ResourceTransfer, LocalSelectionTransfer).
- Optional drag-out to the OS for existing filesystem entries.
- Double-click to open files or reveal folders, plus explicit commands for reveal and copy path.
- Favorites persist as JSON in the instance scope preferences; missing entries are highlighted and tooltipped.
- Background listener keeps entries in sync with workspace move/rename/delete events.

## Commands & Shortcuts
- `Add Current Editor to Favorites` (`M1+M2+F`) – toolbar and command palette.
- `Remove from Favorites` – toolbar, context menu, multi-selection aware.
- Context menu additions: `Open`, `Reveal in Package Explorer`, `Copy Path`, `Remove`, plus a submenu that lists superclasses and inherited interfaces for Java favorites.

## Mini Smoke Test
1. Drag a workspace file from the Package Explorer into Favorites – entry appears.
2. Drag an external file from the OS into Favorites – entry appears.
3. Double-click each entry – files open, folders reveal in Package Explorer.
4. Restart Eclipse – Favorites View still shows the entries.
5. Delete a file on disk – entry turns grey with "Missing" tooltip.

## Troubleshooting
- **Permissions**: Ensure Eclipse can read/write the target files; external locations need filesystem access.
- **Linked resources**: The view resolves workspace-linked files; if links break, entries show as missing until the resource is restored.
- **Package Explorer missing**: If JDT is not installed, the handler falls back to the Project Explorer.

## Building Blocks
- Plug-in bundle `com.holgersiegel.favorites`
- Feature `com.holgersiegel.favorites.feature`
- Update site `com.holgersiegel.favorites.updatesite`

## License

This project is licensed under the **Holger-NonCommercial-Personal-Work-License 1.0**.

- Public use: free of charge, non-commercial only.  
- Author’s exception: only Holger Siegel himself may use it in the course of his
  employment at his current employer.  
- No rights for colleagues, other employees, or the employer as a whole.  

See [LICENSE](./LICENSE.md) for the full text





