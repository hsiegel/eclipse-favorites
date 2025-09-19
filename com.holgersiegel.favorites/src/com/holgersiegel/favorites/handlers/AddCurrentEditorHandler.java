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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.holgersiegel.favorites.model.FavoritesStore;
import com.holgersiegel.favorites.util.FavoritesPlugin;

public class AddCurrentEditorHandler extends AbstractHandler {

    private static final String TITLE = "Favorites";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window == null) {
            return null;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return null;
        }
        IEditorPart editor = page.getActiveEditor();
        if (editor == null) {
            showInfo(window.getShell(), "Kein aktiver Editor.");
            return null;
        }
        FavoritesStore store = FavoritesPlugin.getDefault().getFavoritesStore();
        if (store == null) {
            return null;
        }
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput) {
            IResource resource = ((IFileEditorInput) input).getFile();
            store.addResource(resource);
            return null;
        }
        if (input instanceof IURIEditorInput) {
            URI uri = ((IURIEditorInput) input).getURI();
            if (uri != null && "file".equalsIgnoreCase(uri.getScheme())) {
                Path path = Paths.get(uri);
                store.addExternal(path);
                return null;
            }
        }
        IResource resource = Adapters.adapt(input, IResource.class);
        if (resource != null) {
            store.addResource(resource);
            return null;
        }
        showInfo(window.getShell(), "Der Editor stellt keine Datei bereit.");
        return null;
    }

    private void showInfo(Shell shell, String message) {
        if (shell == null) {
            shell = Display.getDefault().getActiveShell();
        }
        MessageDialog.openInformation(shell, TITLE, message);
    }
}
