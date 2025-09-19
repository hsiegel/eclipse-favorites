/*
 * MIT License
 *
 * Copyright (c) 2025 Holger Siegel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.holgersiegel.favorites.handlers;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.util.FavoritesPlugin;
import com.holgersiegel.favorites.util.Resources;

public class OpenHandler extends AbstractHandler {

    private static final String TITLE = "Favorites";
    private static final String PACKAGE_EXPLORER_ID = "org.eclipse.jdt.ui.PackageExplorer";
    private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection structured = (IStructuredSelection) selection;
        if (structured.isEmpty()) {
            return null;
        }
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (page == null) {
            return null;
        }
        List<IResource> toReveal = new ArrayList<>();
        for (Object element : structured.toArray()) {
            if (element instanceof FavoriteEntry) {
                handleEntry(page, (FavoriteEntry) element, toReveal);
            }
        }
        if (!toReveal.isEmpty()) {
            reveal(page, new StructuredSelection(toReveal));
        }
        return null;
    }

    private void handleEntry(IWorkbenchPage page, FavoriteEntry entry, List<IResource> toReveal) {
        if (entry.getStatus() == FavoriteEntry.Status.MISSING) {
            showInfo("Eintrag ist nicht mehr vorhanden: " + describe(entry));
            return;
        }
        if (entry.isWorkspaceResource()) {
            IResource resource = Resources.resolveWorkspaceResource(entry);
            if (resource == null) {
                showInfo("Workspace-Ressource nicht gefunden: " + describe(entry));
                return;
            }
            if (resource instanceof IFile) {
                openWorkspaceFile(page, (IFile) resource);
            } else if (resource instanceof IContainer) {
                toReveal.add(resource);
            }
            return;
        }
        openExternal(page, entry);
    }

    private void openWorkspaceFile(IWorkbenchPage page, IFile file) {
        try {
            IDE.openEditor(page, file, true);
        } catch (PartInitException ex) {
            log("Datei konnte nicht geöffnet werden: " + file.getName(), ex);
            showInfo("Datei konnte nicht geöffnet werden: " + file.getName());
        }
    }

    private void openExternal(IWorkbenchPage page, FavoriteEntry entry) {
        String rawPath = entry.getAbsolutePath();
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(rawPath);
            if (Files.isDirectory(path)) {
                Program.launch(path.toString());
                return;
            }
            if (!Files.exists(path)) {
                showInfo("Datei nicht gefunden: " + rawPath);
                return;
            }
            IFileStore store = EFS.getLocalFileSystem().getStore(path.toUri());
            IDE.openEditorOnFileStore(page, store);
        } catch (InvalidPathException ex) {
            showInfo("Ungültiger Pfad: " + rawPath);
        } catch (PartInitException ex) {
            log("Kein Editor für Pfad", ex);
            showInfo("Keine passende Editor-Zuordnung für: " + rawPath);
        }
    }

    private void reveal(IWorkbenchPage page, StructuredSelection selection) {
        IViewPart view = showView(page, PACKAGE_EXPLORER_ID);
        if (!(view instanceof ISetSelectionTarget)) {
            view = showView(page, PROJECT_EXPLORER_ID);
        }
        if (view instanceof ISetSelectionTarget) {
            ((ISetSelectionTarget) view).selectReveal(selection);
        }
    }

    private IViewPart showView(IWorkbenchPage page, String viewId) {
        if (page == null) {
            return null;
        }
        try {
            return page.showView(viewId);
        } catch (PartInitException ex) {
            log("View konnte nicht geöffnet werden: " + viewId, ex);
            return null;
        }
    }

    private void showInfo(String message) {
        Shell shell = Display.getDefault().getActiveShell();
        if (shell != null) {
            MessageDialog.openInformation(shell, TITLE, message);
        }
    }

    private void log(String message, Exception ex) {
        FavoritesPlugin plugin = FavoritesPlugin.getDefault();
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.ERROR, FavoritesPlugin.PLUGIN_ID, message, ex));
        }
    }

    private String describe(FavoriteEntry entry) {
        String label = entry.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        String path = entry.getAbsolutePath();
        return path == null ? "<unbekannt>" : path;
    }
}
