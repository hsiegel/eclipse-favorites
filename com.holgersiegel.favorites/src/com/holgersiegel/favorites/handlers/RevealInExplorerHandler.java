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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ISetSelectionTarget;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.util.FavoritesPlugin;
import com.holgersiegel.favorites.util.Resources;

public class RevealInExplorerHandler extends AbstractHandler {

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
        List<IResource> resources = new ArrayList<>();
        for (Object element : structured.toArray()) {
            if (element instanceof FavoriteEntry) {
                FavoriteEntry entry = (FavoriteEntry) element;
                if (entry.isWorkspaceResource()) {
                    IResource resource = Resources.resolveWorkspaceResource(entry);
                    if (resource != null) {
                        resources.add(resource);
                    }
                }
            } else if (element instanceof IResource) {
                resources.add((IResource) element);
            }
        }
        if (resources.isEmpty()) {
            return null;
        }
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (page == null) {
            return null;
        }
        ISelection revealSelection = new StructuredSelection(resources);
        IViewPart view = showView(page, PACKAGE_EXPLORER_ID);
        if (!(view instanceof ISetSelectionTarget)) {
            view = showView(page, PROJECT_EXPLORER_ID);
        }
        if (view instanceof ISetSelectionTarget) {
            ISetSelectionTarget target = (ISetSelectionTarget) view;
            target.selectReveal(revealSelection);
        }
        return null;
    }

    private IViewPart showView(IWorkbenchPage page, String viewId) {
        try {
            return page.showView(viewId);
        } catch (PartInitException ex) {
            FavoritesPlugin plugin = FavoritesPlugin.getDefault();
            if (plugin != null) {
                plugin.getLog().log(ex.getStatus());
            }
            return null;
        }
    }
}
