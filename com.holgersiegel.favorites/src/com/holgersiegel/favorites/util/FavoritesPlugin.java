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
package com.holgersiegel.favorites.util;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.holgersiegel.favorites.model.FavoritesStore;

public class FavoritesPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.holgersiegel.favorites";

    private static FavoritesPlugin plugin;

    private FavoritesStore favoritesStore;

    public FavoritesPlugin() {
    }

    public static FavoritesPlugin getDefault() {
        return plugin;
    }

    public FavoritesStore getFavoritesStore() {
        return favoritesStore;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        favoritesStore = new FavoritesStore();
        favoritesStore.load();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            if (favoritesStore != null) {
                favoritesStore.saveNow();
                favoritesStore.dispose();
            }
        } finally {
            favoritesStore = null;
            plugin = null;
            super.stop(context);
        }
    }
}
