package com.holgersiegel.favorites.menus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.util.FavoritesPlugin;
import com.holgersiegel.favorites.util.Resources;
import com.holgersiegel.favorites.views.FavoritesView;

public class SuperTypeMenuContribution extends CompoundContributionItem implements IWorkbenchContribution {

    private static final String MENU_LABEL = "Superklassen / Interfaces";

    private IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    protected IContributionItem[] getContributionItems() {
        FavoriteEntry entry = findSelectedFavorite();
        List<IType> superTypes = entry == null ? Collections.emptyList() : collectSupertypes(entry);
        final boolean hasTypes = !superTypes.isEmpty();
        MenuManager manager = new MenuManager(MENU_LABEL) {
            @Override
            public boolean isEnabled() {
                return hasTypes;
            }
        };
        if (hasTypes) {
            for (IType type : superTypes) {
                manager.add(createOpenTypeItem(type));
            }
        } else {
            manager.add(new Action("Keine Superklassen oder Interfaces verfügbar") {
                {
                    setEnabled(false);
                }
            });
        }
        return new IContributionItem[] { manager };
    }

    private FavoriteEntry findSelectedFavorite() {
        if (serviceLocator == null) {
            return null;
        }
        IWorkbenchWindow window = serviceLocator.getService(IWorkbenchWindow.class);
        if (window == null) {
            return null;
        }
        ISelectionService selectionService = window.getSelectionService();
        ISelection selection = selectionService.getSelection(FavoritesView.ID);
        if (selection == null) {
            selection = selectionService.getSelection();
        }
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection structured = (IStructuredSelection) selection;
        if (structured.size() != 1) {
            return null;
        }
        Object element = structured.getFirstElement();
        return element instanceof FavoriteEntry ? (FavoriteEntry) element : null;
    }

    private List<IType> collectSupertypes(FavoriteEntry entry) {
        IType type = resolveType(entry);
        if (type == null || !type.exists()) {
            return Collections.emptyList();
        }
        try {
            ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
            List<IType> result = new ArrayList<>();
            collectSuperclasses(type, hierarchy, result);
            collectInterfaces(type, hierarchy, result);
            return result;
        } catch (JavaModelException ex) {
            log("Failed to resolve supertypes", ex);
            return Collections.emptyList();
        }
    }

    private void collectSuperclasses(IType type, ITypeHierarchy hierarchy, List<IType> collector) throws JavaModelException {
        IType current = hierarchy.getSuperclass(type);
        while (current != null && current.exists()) {
            collector.add(current);
            current = hierarchy.getSuperclass(current);
        }
    }

    private void collectInterfaces(IType type, ITypeHierarchy hierarchy, List<IType> collector) throws JavaModelException {
        Set<IType> visited = new LinkedHashSet<>();
        Deque<IType> queue = new ArrayDeque<>();
        Collections.addAll(queue, hierarchy.getSuperInterfaces(type));
        while (!queue.isEmpty()) {
            IType iface = queue.removeFirst();
            if (iface == null || !iface.exists() || !visited.add(iface)) {
                continue;
            }
            collector.add(iface);
            Collections.addAll(queue, hierarchy.getSuperInterfaces(iface));
        }
    }

    private IType resolveType(FavoriteEntry entry) {
        IResource resource = Resources.resolveWorkspaceResource(entry);
        if (resource instanceof IFile) {
            IJavaElement element = JavaCore.create(resource);
            return toPrimaryType(element);
        }
        return null;
    }

    private IType toPrimaryType(IJavaElement javaElement) {
        if (javaElement == null) {
            return null;
        }
        try {
            if (javaElement instanceof ICompilationUnit compilationUnit) {
                IType primary = compilationUnit.findPrimaryType();
                if (primary != null && primary.exists()) {
                    return primary;
                }
                IType[] allTypes = compilationUnit.getAllTypes();
                return allTypes.length > 0 ? allTypes[0] : null;
            }
            if (javaElement instanceof IClassFile classFile) {
                return classFile.getType();
            }
            if (javaElement instanceof IType type) {
                return type;
            }
        } catch (JavaModelException ex) {
            log("Failed to access Java element", ex);
        }
        return null;
    }

    private IContributionItem createOpenTypeItem(IType type) {
        String label = type.getFullyQualifiedName('.');
        Action action = new Action(label) {
            @Override
            public void run() {
                openType(type);
            }
        };
        action.setImageDescriptor(iconFor(type));
        return new ActionContributionItem(action);
    }

    private ImageDescriptor iconFor(IType type) {
        ISharedImages images = JavaUI.getSharedImages();
        String key;
        try {
            key = type.isInterface() ? ISharedImages.IMG_OBJS_INTERFACE : ISharedImages.IMG_OBJS_CLASS;
        } catch (JavaModelException ex) {
            log("Failed to determine type category", ex);
            key = ISharedImages.IMG_OBJS_CLASS;
        }
        return images.getImageDescriptor(key);
    }

    private void openType(IType type) {
        try {
            JavaUI.openInEditor(type);
        } catch (PartInitException | JavaModelException ex) {
            log("Failed to open type in editor", ex);
        }
    }

    private void log(String message, Exception ex) {
        FavoritesPlugin plugin = FavoritesPlugin.getDefault();
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.ERROR, FavoritesPlugin.PLUGIN_ID, message, ex));
        }
    }
}
