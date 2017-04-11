/*
 * Copyright 2017 the original author or authors.
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
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.FileCollectionVisitor;
import org.gradle.api.internal.file.FileTreeInternal;
import org.gradle.api.internal.file.collections.DirectoryFileTree;
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class FileSnapshotTreeFactory {
    private final FileSnapshotFactory fileSnapshotFactory;
    private final FileSystemMirror fileSystemMirror;
    private final DirectoryFileTreeFactory directoryFileTreeFactory;

    public FileSnapshotTreeFactory(FileSnapshotFactory fileSnapshotFactory, FileSystemMirror fileSystemMirror, DirectoryFileTreeFactory directoryFileTreeFactory) {
        this.fileSnapshotFactory = fileSnapshotFactory;
        this.fileSystemMirror = fileSystemMirror;
        this.directoryFileTreeFactory = directoryFileTreeFactory;
    }

    public DefaultFileSnapshotTree fileTree(FileTreeInternal fileTree) {
        List<FileSnapshot> elements = Lists.newArrayList();
        // TODO: If we could get the backing file we could add it as root element
        fileTree.visitTreeOrBackingFile(new FileVisitorImpl(elements));
        return new DefaultFileSnapshotTree(null, elements);
    }

    public DefaultFileSnapshotTree directoryTree(DirectoryFileTree directoryFileTree) {
        List<FileSnapshot> elements;
        FileSnapshot root = fileSnapshotFactory.directorySnapshot(directoryFileTree.getDir());
        if (!directoryFileTree.getPatterns().isEmpty()) {
            // Currently handle only those trees where we want everything from a directory
            elements = Lists.newArrayList();
            directoryFileTree.visit(new FileVisitorImpl(elements));
        } else {
            DirectoryTreeDetails treeDetails = fileSystemMirror.getDirectoryTree(directoryFileTree.getDir().getAbsolutePath());
            if (treeDetails != null) {
                // Reuse the details
                elements = treeDetails.getElements();
            } else {
                // Scan the directory
                elements = Lists.newArrayList();
                directoryFileTree.visit(new FileVisitorImpl(elements));
                DirectoryTreeDetails details = new DirectoryTreeDetails(root.getPath(), ImmutableList.copyOf(elements));
                fileSystemMirror.putDirectory(details);
            }
        }

        return new DefaultFileSnapshotTree(root, elements);
    }

    public List<FileSnapshotTree> fileCollection(FileCollection input) {
        LinkedList<FileSnapshotTree> fileTreeElements = Lists.newLinkedList();
        FileCollectionInternal fileCollection = (FileCollectionInternal) input;
        FileCollectionVisitorImpl visitor = new FileCollectionVisitorImpl(fileTreeElements);
        fileCollection.visitRootElements(visitor);
        return fileTreeElements;
    }

    private class FileCollectionVisitorImpl implements FileCollectionVisitor {
        private final List<FileSnapshotTree> fileTreeElements;

        FileCollectionVisitorImpl(List<FileSnapshotTree> fileTreeElements) {
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
                        fileTreeElements.add(details);
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
            fileTreeElements.add(fileTree(fileTree));
        }

        @Override
        public void visitDirectoryTree(DirectoryFileTree directoryTree) {
            fileTreeElements.add(directoryTree(directoryTree));
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
