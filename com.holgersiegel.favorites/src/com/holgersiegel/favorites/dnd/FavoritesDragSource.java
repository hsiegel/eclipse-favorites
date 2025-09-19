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
package com.holgersiegel.favorites.dnd;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.ui.part.ResourceTransfer;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.util.Resources;

public class FavoritesDragSource extends DragSourceAdapter {

    private final TreeViewer viewer;

    public FavoritesDragSource(TreeViewer viewer) {
        this.viewer = Objects.requireNonNull(viewer, "viewer");
    }

    @Override
    public void dragStart(DragSourceEvent event) {
        IStructuredSelection selection = viewer.getStructuredSelection();
        boolean hasSelection = selection != null && !selection.isEmpty();
        event.doit = hasSelection;
        if (hasSelection) {
            LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
            transfer.setSelection(selection);
            transfer.setSelectionSetTime(System.currentTimeMillis());
        }
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection == null || selection.isEmpty()) {
            return;
        }
        if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
            event.data = selection;
            return;
        }
        if (ResourceTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = extractResources(selection);
            return;
        }
        if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = extractFilePaths(selection);
        }
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
        LocalSelectionTransfer.getTransfer().setSelection(null);
    }

    private IResource[] extractResources(IStructuredSelection selection) {
        List<IResource> resources = new ArrayList<>();
        for (Object element : selection.toArray()) {
            if (element instanceof FavoriteEntry) {
                FavoriteEntry entry = (FavoriteEntry) element;
                IResource resource = Resources.resolveWorkspaceResource(entry);
                if (resource != null) {
                    resources.add(resource);
                }
            } else if (element instanceof IResource) {
                resources.add((IResource) element);
            }
        }
        return resources.toArray(IResource[]::new);
    }

    private String[] extractFilePaths(IStructuredSelection selection) {
        List<String> paths = new ArrayList<>();
        for (Object element : selection.toArray()) {
            if (element instanceof FavoriteEntry) {
                FavoriteEntry entry = (FavoriteEntry) element;
                appendIfExisting(paths, entry.getAbsolutePath());
            } else if (element instanceof IResource) {
                appendIfExisting(paths, Resources.toAbsolutePath((IResource) element));
            }
        }
        return paths.toArray(String[]::new);
    }

    private void appendIfExisting(List<String> paths, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(rawPath);
            if (Files.exists(path)) {
                paths.add(path.toString());
            }
        } catch (InvalidPathException ex) {
            // ignore invalid paths
        }
    }
}
