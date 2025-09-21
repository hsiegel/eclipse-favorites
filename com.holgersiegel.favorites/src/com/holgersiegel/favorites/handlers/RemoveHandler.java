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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ISources;

import com.holgersiegel.favorites.model.FavoriteEntry;
import com.holgersiegel.favorites.model.FavoritesStore;
import com.holgersiegel.favorites.util.FavoritesPlugin;

public class RemoveHandler extends AbstractHandler {

    @Override
    public void setEnabled(Object evaluationContext) {
        ISelection selection = null;
        Object variable = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME);
        if (variable instanceof ISelection) {
            selection = (ISelection) variable;
        }
        boolean enabled = false;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            if (!structured.isEmpty()) {
                enabled = true;
                for (Object element : structured.toArray()) {
                    if (!(element instanceof FavoriteEntry)) {
                        enabled = false;
                        break;
                    }
                }
            }
        }
        setBaseEnabled(enabled);
    }

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
        FavoritesStore store = FavoritesPlugin.getDefault().getFavoritesStore();
        if (store == null) {
            return null;
        }
        List<FavoriteEntry> toRemove = new ArrayList<>();
        for (Object element : structured.toArray()) {
            if (element instanceof FavoriteEntry) {
                toRemove.add((FavoriteEntry) element);
            }
        }
        if (!toRemove.isEmpty()) {
            store.remove(toRemove);
        }
        return null;
    }
}
