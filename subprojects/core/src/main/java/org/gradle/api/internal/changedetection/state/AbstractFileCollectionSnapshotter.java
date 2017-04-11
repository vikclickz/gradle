/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.FileCollectionVisitor;
import org.gradle.api.internal.file.FileTreeInternal;
import org.gradle.api.internal.file.collections.DirectoryFileTree;
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory;
import org.gradle.internal.serialize.SerializerRegistry;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Responsible for calculating a {@link FileCollectionSnapshot} for a particular {@link FileCollection}.
 */
public abstract class AbstractFileCollectionSnapshotter implements FileCollectionSnapshotter {
    private final StringInterner stringInterner;
    private final DirectoryFileTreeFactory directoryFileTreeFactory;
    private final FileSystemMirror fileSystemMirror;
    private final FileSnapshotFactory fileSnapshotFactory;

    public AbstractFileCollectionSnapshotter(FileSnapshotFactory fileSnapshotFactory, StringInterner stringInterner, DirectoryFileTreeFactory directoryFileTreeFactory, FileSystemMirror fileSystemMirror) {
        this.fileSnapshotFactory = fileSnapshotFactory;
        this.stringInterner = stringInterner;
        this.directoryFileTreeFactory = directoryFileTreeFactory;
        this.fileSystemMirror = fileSystemMirror;
    }

    public void registerSerializers(SerializerRegistry registry) {
        registry.register(DefaultFileCollectionSnapshot.class, new DefaultFileCollectionSnapshot.SerializerImpl(stringInterner));
    }

    @Override
    public FileCollectionSnapshot snapshot(FileCollection input, TaskFilePropertyCompareStrategy compareStrategy, final SnapshotNormalizationStrategy snapshotNormalizationStrategy) {
        List<FileSnapshot> fileTreeElements = Lists.newLinkedList();
        FileCollectionInternal fileCollection = (FileCollectionInternal) input;
        FileCollectionVisitorImpl visitor = new FileCollectionVisitorImpl(fileTreeElements);
        fileCollection.visitRootElements(visitor);

        if (fileTreeElements.isEmpty()) {
            return FileCollectionSnapshot.EMPTY;
        }

        Map<String, NormalizedFileSnapshot> snapshots = Maps.newLinkedHashMap();
        for (FileSnapshot fileSnapshot : fileTreeElements) {
            String absolutePath = fileSnapshot.getPath();
            if (!snapshots.containsKey(absolutePath)) {
                NormalizedFileSnapshot normalizedSnapshot = snapshotNormalizationStrategy.getNormalizedSnapshot(fileSnapshot, stringInterner);
                if (normalizedSnapshot != null) {
                    snapshots.put(absolutePath, normalizedSnapshot);
                }
            }
        }
        return new DefaultFileCollectionSnapshot(snapshots, compareStrategy, snapshotNormalizationStrategy.isPathAbsolute());
    }

    private String getPath(File file) {
        return stringInterner.intern(file.getAbsolutePath());
    }

    /**
     * Normalises the elements of a directory tree. Does not include the root directory.
     */
    protected List<FileSnapshot> normaliseTreeElements(List<FileSnapshot> treeNonRootElements) {
        return treeNonRootElements;
    }

    /**
     * Normalises a root file. Invoked only for top level elements that are regular files.
     */
    protected FileSnapshot normaliseFileElement(FileSnapshot details) {
        return details;
    }

    private class FileCollectionVisitorImpl implements FileCollectionVisitor {
        private final List<FileSnapshot> fileTreeElements;

        FileCollectionVisitorImpl(List<FileSnapshot> fileTreeElements) {
            this.fileTreeElements = fileTreeElements;
        }

        @Override
        public void visitCollection(FileCollectionInternal fileCollection) {
            for (File file : fileCollection) {
                FileSnapshot details = fileSnapshotFactory.of(file);
                switch (details.getType()) {
                    case Missing:
                        fileTreeElements.add(details);
                        break;
                    case RegularFile:
                        fileTreeElements.add(normaliseFileElement(details));
                        break;
                    case Directory:
                        // Visit the directory itself, then its contents
                        fileTreeElements.add(details);
                        visitDirectoryTree(directoryFileTreeFactory.create(file));
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        }

        @Override
        public void visitTree(FileTreeInternal fileTree) {
            List<FileSnapshot> elements = Lists.newArrayList();
            fileTree.visitTreeOrBackingFile(new FileVisitorImpl(elements));
            elements = normaliseTreeElements(elements);
            fileTreeElements.addAll(elements);
        }

        @Override
        public void visitDirectoryTree(DirectoryFileTree directoryTree) {
            List<FileSnapshot> elements;
            if (!directoryTree.getPatterns().isEmpty()) {
                // Currently handle only those trees where we want everything from a directory
                elements = Lists.newArrayList();
                directoryTree.visit(new FileVisitorImpl(elements));
            } else {
                DirectoryTreeDetails treeDetails = fileSystemMirror.getDirectoryTree(directoryTree.getDir().getAbsolutePath());
                if (treeDetails != null) {
                    // Reuse the details
                    elements = treeDetails.elements;
                } else {
                    // Scan the directory
                    String path = getPath(directoryTree.getDir());
                    elements = Lists.newArrayList();
                    directoryTree.visit(new FileVisitorImpl(elements));
                    DirectoryTreeDetails details = new DirectoryTreeDetails(path, ImmutableList.copyOf(elements));
                    fileSystemMirror.putDirectory(details);
                }
            }

            elements = normaliseTreeElements(elements);
            fileTreeElements.addAll(elements);
        }
    }

    private class FileVisitorImpl implements FileVisitor {
        private final List<FileSnapshot> fileTreeElements;

        FileVisitorImpl(List<FileSnapshot> fileTreeElements) {
            this.fileTreeElements = fileTreeElements;
        }

        @Override
        public void visitDir(FileVisitDetails dirDetails) {
            fileTreeElements.add(fileSnapshotFactory.directorySnapshot(dirDetails));
        }

        @Override
        public void visitFile(FileVisitDetails fileDetails) {
            fileTreeElements.add(fileSnapshotFactory.fileSnapshot(fileDetails));
        }
    }
}
