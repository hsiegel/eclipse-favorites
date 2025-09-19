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
package com.holgersiegel.favorites.views;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.holgersiegel.favorites.dnd.FavoritesDragSource;
import com.holgersiegel.favorites.dnd.FavoritesDropAdapter;
import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.model.FavoritesStore;
import com.holgersiegel.favorites.model.FavoritesStoreListener;
import com.holgersiegel.favorites.util.FavoritesPlugin;
import com.holgersiegel.favorites.util.Resources;

public class FavoritesView extends ViewPart {

    public static final String ID = "com.holgersiegel.favorites.views.FavoritesView";
    private static final String OPEN_COMMAND_ID = "com.holgersiegel.favorites.commands.open";

    private TreeViewer viewer;
    private FavoritesStore store;
    private FavoritesStoreListener storeListener;

    @Override
    public void createPartControl(Composite parent) {
        store = FavoritesPlugin.getDefault().getFavoritesStore();

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new FavoritesContentProvider());
        FavoritesLabelProvider labelProvider = new FavoritesLabelProvider();
        viewer.setLabelProvider(labelProvider);
        viewer.setUseHashlookup(true);
        viewer.setInput(store);
        ColumnViewerToolTipSupport.enableFor(viewer);

        getSite().setSelectionProvider(viewer);
        registerContextMenu();
        hookDoubleClick();
        hookDragAndDrop();

        storeListener = entries -> asyncRefresh();
        if (store != null) {
            store.addListener(storeListener);
        }
    }

    @Override
    public void setFocus() {
        if (viewer != null) {
            Control control = viewer.getControl();
            if (control != null && !control.isDisposed()) {
                control.setFocus();
            }
        }
    }

    @Override
    public void dispose() {
        if (store != null && storeListener != null) {
            store.removeListener(storeListener);
        }
        if (viewer != null) {
            ColumnLabelProvider labelProvider = (ColumnLabelProvider) viewer.getLabelProvider();
            if (labelProvider instanceof FavoritesLabelProvider) {
                ((FavoritesLabelProvider) labelProvider).disposeResources();
            }
        }
        super.dispose();
    }

    private void registerContextMenu() {
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        IWorkbenchPartSite site = getSite();
        site.registerContextMenu(manager, viewer);
        Menu menu = manager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
    }

    private void hookDoubleClick() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                executeCommand(OPEN_COMMAND_ID);
            }
        });
    }

    private void hookDragAndDrop() {
        Transfer[] transfers = new Transfer[] {
                LocalSelectionTransfer.getTransfer(),
                ResourceTransfer.getInstance(),
                FileTransfer.getInstance()
        };
        int operations = DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE;
        viewer.addDropSupport(operations, transfers, new FavoritesDropAdapter(viewer, store));
        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, new FavoritesDragSource(viewer));
    }

    private void asyncRefresh() {
        if (viewer == null) {
            return;
        }
        Display display = viewer.getControl().getDisplay();
        display.asyncExec(() -> {
            if (viewer.getControl().isDisposed()) {
                return;
            }
            viewer.refresh();
        });
    }

    private void executeCommand(String commandId) {
        IHandlerService handlerService = getSite().getService(IHandlerService.class);
        if (handlerService == null) {
            return;
        }
        try {
            handlerService.executeCommand(commandId, null);
        } catch (Exception ex) {
            FavoritesPlugin plugin = FavoritesPlugin.getDefault();
            if (plugin != null) {
                plugin.getLog().log(new Status(IStatus.ERROR, FavoritesPlugin.PLUGIN_ID, "Command execution failed", ex));
            }
        }
    }

    private static final class FavoritesContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof FavoritesStore) {
                return ((FavoritesStore) parentElement).getEntries().toArray();
            }
            if (parentElement instanceof java.util.Collection<?>) {
                return ((java.util.Collection<?>) parentElement).toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof FavoritesStore) {
                return !((FavoritesStore) element).getEntries().isEmpty();
            }
            return false;
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // no-op
        }

        @Override
        public void dispose() {
            // no-op
        }
    }

    private static final class FavoritesLabelProvider extends ColumnLabelProvider {

        private final Image fileImage;
        private final Image folderImage;
        private final Image projectImage;
        private final Map<Image, Image> missingDecorations = new HashMap<>();
        private final Color missingColor;

        FavoritesLabelProvider() {
            var shared = PlatformUI.getWorkbench().getSharedImages();
            fileImage = shared.getImage(ISharedImages.IMG_OBJ_FILE);
            folderImage = shared.getImage(ISharedImages.IMG_OBJ_FOLDER);
            projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
            missingColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }

        @Override
        public String getText(Object element) {
            if (!(element instanceof FavoriteEntry)) {
                return super.getText(element);
            }
            FavoriteEntry entry = (FavoriteEntry) element;
            String label = entry.getLabel();
            if (label != null && !label.isBlank()) {
                return label;
            }
            String path = entry.getAbsolutePath();
            if (path == null || path.isBlank()) {
                return "<unbekannt>";
            }
            try {
                Path nioPath = Path.of(path);
                Path fileName = nioPath.getFileName();
                return fileName == null ? path : fileName.toString();
            } catch (InvalidPathException ex) {
                return path;
            }
        }

        @Override
        public String getToolTipText(Object element) {
            if (element instanceof FavoriteEntry) {
                FavoriteEntry entry = (FavoriteEntry) element;
                if (entry.getStatus() == FavoriteEntry.Status.MISSING) {
                    return "Missing";
                }
                return entry.getAbsolutePath();
            }
            return super.getToolTipText(element);
        }

        @Override
        public Image getImage(Object element) {
            if (!(element instanceof FavoriteEntry)) {
                return null;
            }
            FavoriteEntry entry = (FavoriteEntry) element;
            Image base = chooseBaseImage(entry);
            if (entry.getStatus() == FavoriteEntry.Status.MISSING) {
                return missingDecorations.computeIfAbsent(base, this::decorateMissing);
            }
            return base;
        }

        @Override
        public Color getForeground(Object element) {
            if (element instanceof FavoriteEntry) {
                FavoriteEntry entry = (FavoriteEntry) element;
                if (entry.getStatus() == FavoriteEntry.Status.MISSING) {
                    return missingColor;
                }
            }
            return super.getForeground(element);
        }

        private Image chooseBaseImage(FavoriteEntry entry) {
            if (entry.isWorkspaceResource()) {
                var resource = Resources.resolveWorkspaceResource(entry);
                if (resource instanceof org.eclipse.core.resources.IProject) {
                    return projectImage;
                }
                if (resource instanceof org.eclipse.core.resources.IContainer) {
                    return folderImage;
                }
            } else if (Resources.isDirectory(entry)) {
                return folderImage;
            }
            return fileImage;
        }

        private Image decorateMissing(Image base) {
            ImageDescriptor overlay = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
            DecorationOverlayIcon icon = new DecorationOverlayIcon(base, overlay, IDecoration.TOP_RIGHT);
            return icon.createImage();
        }

        void disposeResources() {
            for (Image image : missingDecorations.values()) {
                if (image != null && !image.isDisposed()) {
                    image.dispose();
                }
            }
            missingDecorations.clear();
        }
    }
}






