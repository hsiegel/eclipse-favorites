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
package com.holgersiegel.favorites.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.osgi.service.prefs.BackingStoreException;

import com.holgersiegel.favorites.util.FavoritesPlugin;
import com.holgersiegel.favorites.util.Resources;

public class FavoritesStore {

    private static final String PREF_KEY_ENTRIES = "entries";

    private final List<FavoriteEntry> entries = new ArrayList<>();
    private final Map<String, FavoriteEntry> entriesByKey = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<FavoritesStoreListener> listeners = new CopyOnWriteArrayList<>();
    private final IEclipsePreferences preferences;
    private final IResourceChangeListener resourceListener = this::resourceChanged;

    public FavoritesStore() {
        preferences = InstanceScope.INSTANCE.getNode(FavoritesPlugin.PLUGIN_ID);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
    }

    public synchronized List<FavoriteEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public void addListener(FavoritesStoreListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeListener(FavoritesStoreListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized void load() {
        entries.clear();
        entriesByKey.clear();
        String json = preferences.get(PREF_KEY_ENTRIES, "");
        if (json != null && !json.isBlank()) {
            try {
                List<FavoriteEntry> loaded = JsonSupport.read(json);
                for (FavoriteEntry entry : loaded) {
                    internalAdd(entry, false);
                }
            } catch (IllegalArgumentException ex) {
                log("Failed to parse favorites preference", ex);
            }
        }
        refreshStatuses();
        notifyListeners();
    }

    public synchronized boolean addResource(IResource resource) {
        if (resource == null) {
            return false;
        }
        FavoriteEntry entry = new FavoriteEntry(Resources.toAbsolutePath(resource), true, resource.getFullPath().toString(), resource.getName(), FavoriteEntry.Status.OK);
        boolean added = internalAdd(entry, true);
        if (added) {
            refreshStatuses();
            notifyListeners();
        }
        return added;
    }

    public synchronized boolean addExternal(java.nio.file.Path path) {
        if (path == null) {
            return false;
        }
        java.nio.file.Path absolute = path.toAbsolutePath().normalize();
        String name = absolute.getFileName() == null ? absolute.toString() : absolute.getFileName().toString();
        FavoriteEntry entry = new FavoriteEntry(absolute.toString(), false, null, name, FavoriteEntry.Status.OK);
        boolean added = internalAdd(entry, true);
        if (added) {
            refreshStatuses();
            notifyListeners();
        }
        return added;
    }

    public synchronized boolean addEntries(Collection<FavoriteEntry> toAdd) {
        if (toAdd == null || toAdd.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (FavoriteEntry entry : toAdd) {
            if (entry == null) {
                continue;
            }
            if (internalAdd(entry, true)) {
                changed = true;
            }
        }
        if (changed) {
            refreshStatuses();
            notifyListeners();
        }
        return changed;
    }

    public synchronized void remove(Collection<FavoriteEntry> toRemove) {
        if (toRemove == null || toRemove.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (FavoriteEntry entry : toRemove) {
            if (entry == null) {
                continue;
            }
            FavoriteEntry removed = entriesByKey.remove(entry.getKey());
            if (removed != null) {
                entries.remove(removed);
                changed = true;
            }
        }
        if (changed) {
            saveNow();
            notifyListeners();
        }
    }

    public synchronized void move(List<FavoriteEntry> movingEntries, FavoriteEntry target, int location) {
        if (movingEntries == null || movingEntries.isEmpty()) {
            return;
        }
        List<FavoriteEntry> orderedMoving = new ArrayList<>();
        for (FavoriteEntry entry : movingEntries) {
            if (entry == null) {
                continue;
            }
            FavoriteEntry stored = entriesByKey.get(entry.getKey());
            if (stored != null && !orderedMoving.contains(stored)) {
                orderedMoving.add(stored);
            }
        }
        if (orderedMoving.isEmpty()) {
            return;
        }
        FavoriteEntry storedTarget = target == null ? null : entriesByKey.get(target.getKey());
        entries.removeAll(orderedMoving);
        int insertIndex = computeInsertIndex(storedTarget, location);
        entries.addAll(insertIndex, orderedMoving);
        saveNow();
        notifyListeners();
    }

    private int computeInsertIndex(FavoriteEntry target, int location) {
        if (target == null) {
            return entries.size();
        }
        int index = entries.indexOf(target);
        if (index < 0) {
            return entries.size();
        }
        if (location == ViewerDropAdapter.LOCATION_AFTER || location == ViewerDropAdapter.LOCATION_ON) {
            return Math.min(index + 1, entries.size());
        }
        return index;
    }

    public synchronized void saveNow() {
        String json = JsonSupport.write(entries);
        preferences.put(PREF_KEY_ENTRIES, json);
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            log("Failed to persist favorites", ex);
        }
    }

    public synchronized void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
    }

    private boolean internalAdd(FavoriteEntry entry, boolean persist) {
        if (entry == null || entry.getAbsolutePath() == null || entry.getAbsolutePath().isEmpty()) {
            return false;
        }
        String key = entry.getKey();
        FavoriteEntry existing = entriesByKey.get(key);
        if (existing != null) {
            existing.setLabel(entry.getLabel());
            existing.setWorkspacePath(entry.getWorkspacePath());
            existing.setAbsolutePath(entry.getAbsolutePath());
            existing.setStatus(entry.getStatus());
            if (persist) {
                saveNow();
            }
            return false;
        }
        entries.add(entry);
        entriesByKey.put(key, entry);
        if (persist) {
            saveNow();
        }
        return true;
    }

    private void resourceChanged(IResourceChangeEvent event) {
        if (event == null || event.getDelta() == null) {
            return;
        }
        Map<String, IResource> movedResources = new HashMap<>();
        Set<String> removedKeys = new HashSet<>();
        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    IResource resource = delta.getResource();
                    if (resource == null || resource.getType() == IResource.ROOT) {
                        return true;
                    }
                    String absolute = Resources.toAbsolutePath(resource);
                    if (absolute == null) {
                        return true;
                    }
                    String key = Resources.keyFor(absolute);
                    if (delta.getKind() == IResourceDelta.REMOVED) {
                        int flags = delta.getFlags();
                        if ((flags & IResourceDelta.MOVED_TO) != 0) {
                            IResource movedTo = ResourcesPlugin.getWorkspace().getRoot().findMember(delta.getMovedToPath());
                            if (movedTo != null) {
                                movedResources.put(key, movedTo);
                            }
                        } else {
                            removedKeys.add(key);
                        }
                    }
                    if (delta.getKind() == IResourceDelta.ADDED) {
                        removedKeys.remove(key);
                    }
                    return true;
                }
            });
        } catch (CoreException ex) {
            log("Failed to process resource change", ex);
        }
        boolean changed;
        synchronized (this) {
            changed = applyResourceUpdates(movedResources, removedKeys);
            if (refreshStatuses()) {
                changed = true;
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    private boolean applyResourceUpdates(Map<String, IResource> movedResources, Set<String> removedKeys) {
        boolean changed = false;
        for (Map.Entry<String, IResource> moved : movedResources.entrySet()) {
            FavoriteEntry entry = entriesByKey.remove(moved.getKey());
            if (entry != null) {
                IResource target = moved.getValue();
                entry.setAbsolutePath(Resources.toAbsolutePath(target));
                entry.setWorkspacePath(target.getFullPath().toString());
                entry.setLabel(target.getName());
                entry.setStatus(Resources.exists(entry) ? FavoriteEntry.Status.OK : FavoriteEntry.Status.MISSING);
                entriesByKey.put(entry.getKey(), entry);
                changed = true;
            }
        }
        for (String removedKey : removedKeys) {
            FavoriteEntry entry = entriesByKey.get(removedKey);
            if (entry != null && !entry.isMissing()) {
                entry.setStatus(FavoriteEntry.Status.MISSING);
                changed = true;
            }
        }
        if (changed) {
            saveNow();
        }
        return changed;
    }

    private boolean refreshStatuses() {
        boolean changed = false;
        for (FavoriteEntry entry : entries) {
            boolean exists = Resources.exists(entry);
            FavoriteEntry.Status newStatus = exists ? FavoriteEntry.Status.OK : FavoriteEntry.Status.MISSING;
            if (entry.getStatus() != newStatus) {
                entry.setStatus(newStatus);
                changed = true;
            }
        }
        if (changed) {
            saveNow();
        }
        return changed;
    }

    private void notifyListeners() {
        List<FavoriteEntry> snapshot = getEntries();
        for (FavoritesStoreListener listener : listeners) {
            safeExecute(listener::entriesChanged, snapshot);
        }
    }

    private void safeExecute(Consumer<List<FavoriteEntry>> consumer, List<FavoriteEntry> data) {
        try {
            consumer.accept(data);
        } catch (Exception ex) {
            log("Favorites listener failed", ex);
        }
    }

    private void log(String message, Throwable t) {
        FavoritesPlugin plugin = FavoritesPlugin.getDefault();
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.ERROR, FavoritesPlugin.PLUGIN_ID, message, t));
        }
    }

    private static final class JsonSupport {

        static String write(List<FavoriteEntry> entries) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (FavoriteEntry entry : entries) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('{');
                append(sb, "path", entry.getAbsolutePath());
                sb.append(',');
                append(sb, "workspace", entry.isWorkspaceResource());
                sb.append(',');
                append(sb, "workspacePath", entry.getWorkspacePath());
                sb.append(',');
                append(sb, "label", entry.getLabel());
                sb.append(',');
                append(sb, "status", entry.getStatus().name());
                sb.append('}');
            }
            sb.append(']');
            return sb.toString();
        }

        static List<FavoriteEntry> read(String json) {
            Parser parser = new Parser(json);
            return parser.parseArray();
        }

        private static void append(StringBuilder sb, String name, String value) {
            sb.append('"').append(escape(name)).append('"').append(':');
            if (value == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escape(value)).append('"');
            }
        }

        private static void append(StringBuilder sb, String name, boolean value) {
            sb.append('"').append(escape(name)).append('"').append(':').append(value);
        }

        private static String escape(String text) {
            if (text == null) {
                return "";
            }
            StringBuilder escaped = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\' || c == '"') {
                    escaped.append('\\');
                }
                escaped.append(c);
            }
            return escaped.toString();
        }

        private static final class Parser {

            private final StringReader reader;
            private int next = -2;

            Parser(String json) {
                this.reader = new StringReader(json == null ? "" : json);
            }

            List<FavoriteEntry> parseArray() {
                skipWhitespace();
                if (peek() != '[') {
                    throw new IllegalArgumentException("Expected '[' at start of favorites JSON");
                }
                consume();
                List<FavoriteEntry> result = new ArrayList<>();
                skipWhitespace();
                if (peek() == ']') {
                    consume();
                    return result;
                }
                while (true) {
                    result.add(parseObject());
                    skipWhitespace();
                    char c = peek();
                    if (c == ',') {
                        consume();
                        skipWhitespace();
                        continue;
                    }
                    if (c == ']') {
                        consume();
                        break;
                    }
                    throw new IllegalArgumentException("Expected ',' or ']' in favorites JSON array");
                }
                return result;
            }

            private FavoriteEntry parseObject() {
                skipWhitespace();
                if (peek() != '{') {
                    throw new IllegalArgumentException("Expected '{' in favorites JSON");
                }
                consume();
                Map<String, String> values = new HashMap<>();
                skipWhitespace();
                if (peek() == '}') {
                    consume();
                } else {
                    while (true) {
                        String key = parseString();
                        skipWhitespace();
                        if (consume() != ':') {
                            throw new IllegalArgumentException("Expected ':' after key");
                        }
                        skipWhitespace();
                        char nextChar = peek();
                        String value;
                        if (nextChar == 'n') {
                            expectLiteral("null");
                            value = null;
                        } else if (nextChar == 't' || nextChar == 'f') {
                            value = parseLiteral();
                        } else if (nextChar == '"') {
                            value = parseString();
                        } else {
                            throw new IllegalArgumentException("Unexpected value in favorites JSON");
                        }
                        values.put(key, value);
                        skipWhitespace();
                        char separator = peek();
                        if (separator == ',') {
                            consume();
                            skipWhitespace();
                            continue;
                        }
                        if (separator == '}') {
                            consume();
                            break;
                        }
                        throw new IllegalArgumentException("Expected ',' or '}' in favorites JSON object");
                    }
                }
                String path = values.get("path");
                boolean workspace = Boolean.parseBoolean(values.getOrDefault("workspace", "false"));
                String workspacePath = values.get("workspacePath");
                String label = values.get("label");
                String statusValue = values.get("status");
                FavoriteEntry.Status status = statusValue == null ? FavoriteEntry.Status.OK : FavoriteEntry.Status.valueOf(statusValue.toUpperCase(Locale.ENGLISH));
                return new FavoriteEntry(path, workspace, workspacePath, label, status);
            }

            private String parseLiteral() {
                StringBuilder literal = new StringBuilder();
                char c = peek();
                while (Character.isLetter(c)) {
                    literal.append(consume());
                    c = peek();
                }
                return literal.toString();
            }

            private void expectLiteral(String expected) {
                for (int i = 0; i < expected.length(); i++) {
                    if (consume() != expected.charAt(i)) {
                        throw new IllegalArgumentException("Invalid literal in favorites JSON");
                    }
                }
            }

            private String parseString() {
                if (consume() != '"') {
                    throw new IllegalArgumentException("Expected string");
                }
                StringBuilder sb = new StringBuilder();
                while (true) {
                    char c = consume();
                    if (c == '"') {
                        break;
                    }
                    if (c == '\\') {
                        char escaped = consume();
                        sb.append(escaped);
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            private void skipWhitespace() {
                char c = peek();
                while (Character.isWhitespace(c)) {
                    consume();
                    c = peek();
                }
            }

            private char peek() {
                if (next == -2) {
                    next = read();
                }
                return (char) (next == -1 ? 0 : next);
            }

            private char consume() {
                if (next == -2) {
                    next = read();
                }
                if (next == -1) {
                    return 0;
                }
                char current = (char) next;
                next = read();
                return current;
            }

            private int read() {
                try {
                    return reader.read();
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException("Failed to read favorites JSON", ex);
                }
            }
        }
    }
}
