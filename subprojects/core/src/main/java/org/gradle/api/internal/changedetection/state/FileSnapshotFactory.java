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

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.hash.FileHasher;
import org.gradle.internal.nativeintegration.filesystem.FileMetadataSnapshot;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;

import java.io.File;

import static org.gradle.internal.nativeintegration.filesystem.FileType.*;

public class FileSnapshotFactory {
    private final FileSystem fileSystem;
    private final FileSystemMirror fileSystemMirror;
    private final StringInterner stringInterner;
    private final FileHasher hasher;

    public FileSnapshotFactory(FileSystem fileSystem, FileSystemMirror fileSystemMirror, StringInterner stringInterner, FileHasher hasher) {
        this.fileSystem = fileSystem;
        this.fileSystemMirror = fileSystemMirror;
        this.stringInterner = stringInterner;
        this.hasher = hasher;
    }

    public FileSnapshot of(File file) {
        FileSnapshot details = fileSystemMirror.getFile(file.getPath());
        if (details == null) {
            details = calculateFileSnapshot(file);
            fileSystemMirror.putFile(details);
        }
        return details;
    }

    public FileSnapshot fileSnapshot(org.gradle.api.file.FileTreeElement fileDetails) {
        return new DefaultFileSnapshot(getPath(fileDetails.getFile()), fileDetails.getRelativePath(), RegularFile, false, fileHashSnapshot(fileDetails));
    }

    public FileSnapshot directorySnapshot(FileVisitDetails dirDetails) {
        return new DefaultFileSnapshot(getPath(dirDetails.getFile()), dirDetails.getRelativePath(), Directory, false, dirSnapshot());
    }

    private FileSnapshot calculateFileSnapshot(File file) {
        String path = getPath(file);
        FileMetadataSnapshot stat = fileSystem.stat(file);
        switch (stat.getType()) {
            case Missing:
                return new DefaultFileSnapshot(path, new RelativePath(true, file.getName()), Missing, true, missingFileSnapshot());
            case Directory:
                return new DefaultFileSnapshot(path, new RelativePath(false, file.getName()), Directory, true, dirSnapshot());
            case RegularFile:
                return new DefaultFileSnapshot(path, new RelativePath(true, file.getName()), RegularFile, true, fileSnapshot(file, stat));
            default:
                throw new IllegalArgumentException("Unrecognized file type: " + stat.getType());
        }
    }

    private DirSnapshot dirSnapshot() {
        return DirSnapshot.getInstance();
    }

    private MissingFileSnapshot missingFileSnapshot() {
        return MissingFileSnapshot.getInstance();
    }

    private FileHashSnapshot fileHashSnapshot(org.gradle.api.file.FileTreeElement fileDetails) {
        return new FileHashSnapshot(hasher.hash(fileDetails), fileDetails.getLastModified());
    }

    private FileHashSnapshot fileSnapshot(File file, FileMetadataSnapshot fileDetails) {
        return new FileHashSnapshot(hasher.hash(file, fileDetails), fileDetails.getLastModified());
    }

    private String getPath(File file) {
        return stringInterner.intern(file.getAbsolutePath());
    }
}
