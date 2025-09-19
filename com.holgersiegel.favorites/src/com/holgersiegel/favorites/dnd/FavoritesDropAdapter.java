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

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ResourceTransfer;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.model.FavoritesStore;
import com.holgersiegel.favorites.util.Resources;

public class FavoritesDropAdapter extends ViewerDropAdapter {

    private final FavoritesStore store;

    public FavoritesDropAdapter(TreeViewer viewer, FavoritesStore store) {
        super(viewer);
        this.store = Objects.requireNonNull(store, "store");
        setFeedbackEnabled(true);
    }

    @Override
    public boolean validateDrop(Object target, int operation, TransferData transferType) {
        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            setDropOperation(DND.DROP_MOVE);
            return true;
        }
        if (ResourceTransfer.getInstance().isSupportedType(transferType)) {
            setDropOperation(DND.DROP_COPY);
            return true;
        }
        if (FileTransfer.getInstance().isSupportedType(transferType)) {
            setDropOperation(DND.DROP_COPY);
            return true;
        }
        return false;
    }

    @Override
    public boolean performDrop(Object data) {
        TransferData currentType = getCurrentType();
        if (currentType != null && LocalSelectionTransfer.getTransfer().isSupportedType(currentType)) {
            return handleLocalSelection(LocalSelectionTransfer.getTransfer().getSelection());
        }
        if (currentType != null && ResourceTransfer.getInstance().isSupportedType(currentType)) {
            return handleResources(asResourceArray(data));
        }
        if (currentType != null && FileTransfer.getInstance().isSupportedType(currentType)) {
            return handleFileDrop(asStringArray(data));
        }
        if (data instanceof IResource[]) {
            return handleResources((IResource[]) data);
        }
        if (data instanceof String[]) {
            return handleFileDrop((String[]) data);
        }
        return false;
    }

    private boolean handleLocalSelection(ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return false;
        }
        IStructuredSelection structured = (IStructuredSelection) selection;
        List<FavoriteEntry> favorites = new ArrayList<>();
        List<IResource> resources = new ArrayList<>();
        for (Object element : structured.toArray()) {
            if (element instanceof FavoriteEntry) {
                favorites.add((FavoriteEntry) element);
            } else {
                IResource resource = Adapters.adapt(element, IResource.class);
                if (resource != null) {
                    resources.add(resource);
                }
            }
        }
        if (!favorites.isEmpty()) {
            store.move(favorites, (FavoriteEntry) getCurrentTarget(), getCurrentLocation());
            return true;
        }
        boolean changed = false;
        for (IResource resource : resources) {
            changed |= store.addResource(resource);
        }
        return changed;
    }

    private boolean handleResources(IResource[] resources) {
        if (resources == null) {
            return false;
        }
        boolean changed = false;
        for (IResource resource : resources) {
            changed |= store.addResource(resource);
        }
        return changed;
    }

    private boolean handleFileDrop(String[] filePaths) {
        if (filePaths == null) {
            return false;
        }
        boolean changed = false;
        for (String path : filePaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            try {
                changed |= store.addExternal(Paths.get(path));
            } catch (InvalidPathException ex) {
                // ignore invalid paths from the OS drop source
            }
        }
        return changed;
    }

    private IResource[] asResourceArray(Object data) {
        return data instanceof IResource[] ? (IResource[]) data : null;
    }

    private String[] asStringArray(Object data) {
        return data instanceof String[] ? (String[]) data : null;
    }

    private void setDropOperation(int operation) {
        if (getCurrentEvent() != null) {
            getCurrentEvent().detail = operation;
        }
    }

    private TransferData getCurrentType() {
        return getCurrentEvent() != null ? getCurrentEvent().currentDataType : null;
    }
}

