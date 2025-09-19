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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.holgersiegel.favorites.model.FavoriteEntry;

public final class Resources {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).contains("win");

    private Resources() {
    }

    public static String normalize(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return "";
        }
        try {
            Path normalized = Path.of(absolutePath).normalize();
            return normalized.toString();
        } catch (Exception ex) {
            return absolutePath;
        }
    }

    public static String keyFor(String absolutePath) {
        String normalized = normalize(absolutePath);
        return WINDOWS ? normalized.toLowerCase(Locale.ENGLISH) : normalized;
    }

    public static Optional<Path> toPath(FavoriteEntry entry) {
        String absolutePath = entry.getAbsolutePath();
        if (absolutePath == null || absolutePath.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(absolutePath));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static String toAbsolutePath(IResource resource) {
        if (resource == null) {
            return null;
        }
        IPath location = resource.getLocation();
        if (location != null) {
            return location.toOSString();
        }
        URI uri = resource.getLocationURI();
        if (uri != null) {
            try {
                return Paths.get(uri).toString();
            } catch (Exception ex) {
                return uri.getPath();
            }
        }
        return resource.getFullPath().toString();
    }

    public static boolean exists(FavoriteEntry entry) {
        if (entry.isWorkspaceResource()) {
            IResource resource = resolveWorkspaceResource(entry);
            return resource != null && resource.exists();
        }
        return toPath(entry).map(Files::exists).orElse(false);
    }

    public static boolean isDirectory(FavoriteEntry entry) {
        if (entry.isWorkspaceResource()) {
            IResource resource = resolveWorkspaceResource(entry);
            return resource != null && resource.getType() != IResource.FILE;
        }
        return toPath(entry).map(Files::isDirectory).orElse(false);
    }

    public static IResource resolveWorkspaceResource(FavoriteEntry entry) {
        if (!entry.isWorkspaceResource()) {
            return null;
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String workspacePath = entry.getWorkspacePath();
        if (workspacePath != null && !workspacePath.isEmpty()) {
            return root.findMember(new org.eclipse.core.runtime.Path(workspacePath));
        }
        String absolutePath = entry.getAbsolutePath();
        if (absolutePath == null || absolutePath.isBlank()) {
            return null;
        }
        try {
            Path nioPath = Path.of(absolutePath);
            IResource[] files = root.findFilesForLocationURI(nioPath.toUri());
            if (files != null && files.length > 0) {
                return files[0];
            }
            IContainer[] containers = root.findContainersForLocationURI(nioPath.toUri());
            if (containers != null && containers.length > 0) {
                return containers[0];
            }
        } catch (InvalidPathException ex) {
            return null;
        }
        return null;
    }
}
