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

import java.util.Objects;

import com.holgersiegel.favorites.util.Resources;

public class FavoriteEntry {

    public enum Status {
        OK,
        MISSING
    }

    private String absolutePath;
    private final boolean workspaceResource;
    private String workspacePath;
    private String label;
    private Status status;

    public FavoriteEntry(String absolutePath, boolean workspaceResource, String workspacePath, String label, Status status) {
        this.absolutePath = absolutePath;
        this.workspaceResource = workspaceResource;
        this.workspacePath = workspacePath;
        this.label = label;
        this.status = status == null ? Status.OK : status;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public boolean isWorkspaceResource() {
        return workspaceResource;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.OK : status;
    }

    public boolean isMissing() {
        return status == Status.MISSING;
    }

    public String getKey() {
        return Resources.keyFor(absolutePath);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FavoriteEntry)) {
            return false;
        }
        FavoriteEntry other = (FavoriteEntry) obj;
        return Objects.equals(getKey(), other.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @Override
    public String toString() {
        return (workspaceResource ? "workspace:" : "external:") + absolutePath;
    }
}
