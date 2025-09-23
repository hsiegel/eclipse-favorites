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

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;
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
    private static final String ADD_COMMAND_ID = "com.holgersiegel.favorites.commands.addCurrentEditor";
    private static final String REMOVE_COMMAND_ID = "com.holgersiegel.favorites.commands.removeSelected";

    private TreeViewer viewer;
    private FavoritesLabelProvider labelProvider;
    private IPartListener2 partListener;
    private String currentEditorKey;
    private FavoritesStore store;
    private FavoritesStoreListener storeListener;
    private TreeColumn commentColumn;
    private static final int MIN_COMMENT_WIDTH = 200;
    private ISelectionChangedListener handlerUpdateListener;
    private Action addToolbarAction;
    private Action removeToolbarAction;
    private IEvaluationService evaluationService;

    @Override
    public void createPartControl(Composite parent) {
        store = FavoritesPlugin.getDefault().getFavoritesStore();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        viewer = new TreeViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.setContentProvider(new FavoritesContentProvider());
        labelProvider = new FavoritesLabelProvider();
        viewer.setLabelProvider(labelProvider);
        viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        createColumns();
        viewer.setColumnProperties(new String[] { "name", "comment" });
        configureEditing();
        hideTreeHeader();
        viewer.setUseHashlookup(true);
        viewer.setInput(store);
        ColumnViewerToolTipSupport.enableFor(viewer);

        getSite().setSelectionProvider(viewer);
        configureToolbar();
        registerContextMenu();
        hookDoubleClick();
        hookDragAndDrop();
        hookHandlerUpdates();
        hookEditorTracking();

        storeListener = entries -> asyncRefresh();
        if (store != null) {
            store.addListener(storeListener);
        }
    }

    private void createColumns() {
        Tree tree = viewer.getTree();
        tree.setLinesVisible(true);

        TreeViewerColumn nameViewerColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        TreeColumn nameColumn = nameViewerColumn.getColumn();
        nameColumn.setText("Name");
        nameColumn.setResizable(true);
        nameColumn.setMoveable(false);
        nameColumn.setWidth(240);
        nameViewerColumn.setLabelProvider(labelProvider);

        TreeViewerColumn commentViewerColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        commentColumn = commentViewerColumn.getColumn();
        commentColumn.setText("Kommentar");
        commentColumn.setResizable(true);
        commentColumn.setMoveable(false);
        commentColumn.setWidth(320);
        commentViewerColumn.setLabelProvider(new CommentLabelProvider());
        commentViewerColumn.setEditingSupport(new CommentEditingSupport(viewer));

        tree.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                adjustCommentColumnWidth();
            }
        });
        adjustCommentColumnWidth();
        hideTreeHeader();
    }

    private void hideTreeHeader() {
        Tree tree = viewer.getTree();
        if (tree != null && !tree.isDisposed()) {
            tree.setHeaderVisible(false);
        }
    }

    private void configureEditing() {
        ColumnViewerEditorActivationStrategy activationStrategy = new ColumnViewerEditorActivationStrategy(viewer) {
            @Override
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                int type = event.eventType;
                if (type == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) {
                    return false;
                }
                if (type == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION) {
                    if (event.sourceEvent instanceof MouseEvent) {
                        MouseEvent mouseEvent = (MouseEvent) event.sourceEvent;
                        return mouseEvent.button == 1 && mouseEvent.count == 1;
                    }
                    return true;
                }
                if (type == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.sourceEvent instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event.sourceEvent;
                    if (keyEvent.keyCode == SWT.F2 || keyEvent.keyCode == SWT.CR) {
                        return true;
                    }
                    return !Character.isISOControl(keyEvent.character);
                }
                if (type == ColumnViewerEditorActivationEvent.PROGRAMMATIC) {
                    return true;
                }
                return super.isEditorActivationEvent(event);
            }
        };
        TreeViewerEditor.create(viewer, activationStrategy,
                ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                        | ColumnViewerEditor.KEYBOARD_ACTIVATION);
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

    private void adjustCommentColumnWidth() {
        if (commentColumn == null || commentColumn.isDisposed()) {
            return;
        }
        Tree tree = commentColumn.getParent();
        if (tree == null || tree.isDisposed()) {
            return;
        }
        Rectangle area = tree.getClientArea();
        if (area.width <= 0) {
            return;
        }
        int otherWidth = 0;
        for (TreeColumn column : tree.getColumns()) {
            if (column == commentColumn) {
                continue;
            }
            otherWidth += column.getWidth();
        }
        int available = area.width - otherWidth;
        if (available < MIN_COMMENT_WIDTH) {
            available = MIN_COMMENT_WIDTH;
        }
        int current = commentColumn.getWidth();
        if (Math.abs(available - current) > 2) {
            commentColumn.setWidth(available);
        }
    }

    @Override
    public void dispose() {
        if (store != null && storeListener != null) {
            store.removeListener(storeListener);
        }
        if (partListener != null) {
            IWorkbenchPartSite site = getSite();
            if (site != null) {
                IWorkbenchWindow window = site.getWorkbenchWindow();
                if (window != null) {
                    window.getPartService().removePartListener(partListener);
                }
            }
            partListener = null;
        }
        if (viewer != null && handlerUpdateListener != null) {
            viewer.removeSelectionChangedListener(handlerUpdateListener);
            handlerUpdateListener = null;
        }
        if (labelProvider != null) {
            labelProvider.disposeResources();
            labelProvider = null;
        }
        addToolbarAction = null;
        removeToolbarAction = null;
        evaluationService = null;
        currentEditorKey = null;
        super.dispose();
    }

    private void configureToolbar() {
        IViewSite viewSite = getViewSite();
        if (viewSite == null) {
            return;
        }
        var actionBars = viewSite.getActionBars();
        if (actionBars == null) {
            return;
        }
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        if (toolBarManager == null) {
            return;
        }
        var sharedImages = PlatformUI.getWorkbench().getSharedImages();
        ImageDescriptor addIcon = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_ADD);
        ImageDescriptor removeIcon = sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE);
        String addActionId = toolbarItemId(ADD_COMMAND_ID);
        toolBarManager.remove(addActionId);
        addToolbarAction = createCommandAction(addActionId, ADD_COMMAND_ID, "Add Current Editor", addIcon);
        toolBarManager.add(addToolbarAction);
        String removeActionId = toolbarItemId(REMOVE_COMMAND_ID);
        toolBarManager.remove(removeActionId);
        removeToolbarAction = createCommandAction(removeActionId, REMOVE_COMMAND_ID, "Remove", removeIcon);
        toolBarManager.add(removeToolbarAction);
        toolBarManager.update(true);
        actionBars.updateActionBars();
        updateRemoveEnablement();
    }

    private Action createCommandAction(String itemId, String commandId, String label, ImageDescriptor icon) {
        Action action = new Action(label) {
            @Override
            public void run() {
                executeCommand(commandId);
            }
        };
        action.setId(itemId);
        action.setToolTipText(label);
        if (icon != null) {
            action.setImageDescriptor(icon);
            action.setDisabledImageDescriptor(ImageDescriptor.createWithFlags(icon, SWT.IMAGE_DISABLE));
        }
        return action;
    }

    private static String toolbarItemId(String commandId) {
        return commandId + ".toolbar";
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
                plugin.getLog().log(new Status(IStatus.ERROR, FavoritesPlugin.PLUGIN_ID,
                        "Command execution failed: " + commandId, ex));
            }
        }
    }

    private void hookHandlerUpdates() {
        evaluationService = getSite().getService(IEvaluationService.class);
        if (viewer == null) {
            return;
        }
        if (handlerUpdateListener != null) {
            viewer.removeSelectionChangedListener(handlerUpdateListener);
        }
        handlerUpdateListener = event -> updateRemoveEnablement();
        viewer.addSelectionChangedListener(handlerUpdateListener);
        updateRemoveEnablement();
    }

    private void hookEditorTracking() {
        IWorkbenchPartSite site = getSite();
        if (site == null) {
            return;
        }
        IWorkbenchWindow window = site.getWorkbenchWindow();
        if (window == null) {
            return;
        }
        if (partListener != null) {
            window.getPartService().removePartListener(partListener);
        }
        final IWorkbenchWindow workbenchWindow = window;
        partListener = new IPartListener2() {
            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
                updateCurrentEditor(partRef);
            }

            @Override
            public void partBroughtToTop(IWorkbenchPartReference partRef) {
                updateCurrentEditor(partRef);
            }

            @Override
            public void partClosed(IWorkbenchPartReference partRef) {
                updateCurrentEditor(workbenchWindow.getActivePage());
            }

            @Override
            public void partDeactivated(IWorkbenchPartReference partRef) {
                // no-op
            }

            @Override
            public void partHidden(IWorkbenchPartReference partRef) {
                updateCurrentEditor(workbenchWindow.getActivePage());
            }

            @Override
            public void partInputChanged(IWorkbenchPartReference partRef) {
                updateCurrentEditor(partRef);
            }

            @Override
            public void partOpened(IWorkbenchPartReference partRef) {
                updateCurrentEditor(partRef);
            }

            @Override
            public void partVisible(IWorkbenchPartReference partRef) {
                updateCurrentEditor(partRef);
            }
        };
        window.getPartService().addPartListener(partListener);
        updateCurrentEditor(window.getActivePage());
    }

    private void updateCurrentEditor(IWorkbenchPartReference partRef) {
        if (partRef == null) {
            return;
        }
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof IEditorPart) {
            applyCurrentEditorKey(extractEditorKey((IEditorPart) part));
        }
    }

    private void updateCurrentEditor(IWorkbenchPage page) {
        if (page == null) {
            applyCurrentEditorKey(null);
            return;
        }
        applyCurrentEditorKey(extractEditorKey(page.getActiveEditor()));
    }

    private void applyCurrentEditorKey(String newKey) {
        if (newKey != null && newKey.isBlank()) {
            newKey = null;
        }
        if (Objects.equals(currentEditorKey, newKey)) {
            return;
        }
        currentEditorKey = newKey;
        refreshHighlight();
    }

    private String extractEditorKey(IEditorPart editor) {
        if (editor == null) {
            return null;
        }
        IEditorInput input = editor.getEditorInput();
        if (input == null) {
            return null;
        }
        String absolutePath = resolveAbsolutePath(input);
        if (absolutePath == null || absolutePath.isBlank()) {
            return null;
        }
        return Resources.keyFor(absolutePath);
    }

    private String resolveAbsolutePath(IEditorInput input) {
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            return Resources.toAbsolutePath(file);
        }
        IResource resource = (IResource) input.getAdapter(IResource.class);
        if (resource != null) {
            return Resources.toAbsolutePath(resource);
        }
        if (input instanceof IPathEditorInput) {
            IPath pathValue = ((IPathEditorInput) input).getPath();
            if (pathValue != null) {
                String osString = pathValue.toOSString();
                if (osString != null && !osString.isBlank()) {
                    return osString;
                }
            }
        }
        if (input instanceof IURIEditorInput) {
            URI uri = ((IURIEditorInput) input).getURI();
            if (uri != null) {
                try {
                    if ("file".equalsIgnoreCase(uri.getScheme())) {
                        return Paths.get(uri).toString();
                    }
                    return uri.getPath();
                } catch (Exception ex) {
                    return uri.getPath();
                }
            }
        }
        return null;
    }

    private void refreshHighlight() {
        if (viewer == null) {
            return;
        }
        Control control = viewer.getControl();
        if (control == null || control.isDisposed()) {
            return;
        }
        Display display = control.getDisplay();
        display.asyncExec(() -> {
            if (!control.isDisposed()) {
                viewer.refresh();
            }
        });
    }

    private boolean isFavoriteOfCurrentEditor(FavoriteEntry entry) {
        if (entry == null) {
            return false;
        }
        if (currentEditorKey == null || currentEditorKey.isEmpty()) {
            return false;
        }
        String entryKey = entry.getKey();
        return entryKey != null && entryKey.equals(currentEditorKey);
    }

    private void updateRemoveEnablement() {
        boolean hasSelection = hasFavoriteSelection();
        if (removeToolbarAction != null) {
            removeToolbarAction.setEnabled(hasSelection);
        }
        if (evaluationService != null) {
            evaluationService.requestEvaluation(ISources.ACTIVE_CURRENT_SELECTION_NAME);
        }
    }

    private boolean hasFavoriteSelection() {
        if (viewer == null) {
            return false;
        }
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection == null || selection.isEmpty()) {
            return false;
        }
        for (Object element : selection.toArray()) {
            if (!(element instanceof FavoriteEntry)) {
                return false;
            }
        }
        return true;
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
            updateRemoveEnablement();
        });
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

    private static final class CommentLabelProvider extends ColumnLabelProvider {

        @Override
        public String getText(Object element) {
            if (element instanceof FavoriteEntry) {
                String comment = ((FavoriteEntry) element).getComment();
                return comment == null ? "" : comment;
            }
            return super.getText(element);
        }
    }

    private final class CommentEditingSupport extends EditingSupport {

        private final TextCellEditor editor;

        CommentEditingSupport(TreeViewer viewer) {
            super(viewer);
            editor = new TextCellEditor(viewer.getTree());
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return editor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return element instanceof FavoriteEntry;
        }

        @Override
        protected Object getValue(Object element) {
            if (element instanceof FavoriteEntry) {
                String comment = ((FavoriteEntry) element).getComment();
                return comment == null ? "" : comment;
            }
            return "";
        }

        @Override
        protected void setValue(Object element, Object value) {
            if (!(element instanceof FavoriteEntry)) {
                return;
            }
            FavoriteEntry entry = (FavoriteEntry) element;
            String newComment = value == null ? null : value.toString();
            String normalized = newComment == null || newComment.isBlank() ? null : newComment;
            if (Objects.equals(entry.getComment(), normalized)) {
                return;
            }
            if (store != null) {
                store.updateComment(entry, newComment);
            } else {
                entry.setComment(newComment);
            }
            getViewer().update(entry, null);
        }
    }

    private final class FavoritesLabelProvider extends ColumnLabelProvider {

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

        @Override
        public Font getFont(Object element) {
            if (element instanceof FavoriteEntry && isFavoriteOfCurrentEditor((FavoriteEntry) element)) {
                return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
            }
            return super.getFont(element);
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

