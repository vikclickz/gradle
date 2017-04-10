package org.gradle.snapshotting.contexts;

import com.google.common.hash.HashCode;
import org.gradle.internal.hash.HashUtil;
import org.gradle.snapshotting.files.Fileish;
import org.gradle.snapshotting.files.MissingPhysicalFile;
import org.gradle.snapshotting.files.Physical;
import org.gradle.snapshotting.files.PhysicalDirectory;

public abstract class Result implements Comparable<Result> {
    private final Fileish file;
    private final String normalizedPath;

    public Result(Fileish file, String normalizedPath) {
        this.file = file;
        this.normalizedPath = normalizedPath;
    }

    public HashCode fold(PhysicalSnapshotCollector physicalSnapshots) {
        HashCode hashCode = foldInternal(physicalSnapshots);
        if (file instanceof Physical) {
            HashCode physicalHash;
            if (file instanceof PhysicalDirectory) {
                physicalHash = PhysicalDirectory.HASH;
            } else if (file instanceof MissingPhysicalFile) {
                physicalHash = MissingPhysicalFile.HASH;
            } else {
                physicalHash = hashCode;
            }
            physicalSnapshots.collectSnapshot((Physical) file, getNormalizedPath(), physicalHash);
        }
        return hashCode;
    }

    public abstract HashCode foldInternal(PhysicalSnapshotCollector physicalSnapshots);

    public Fileish getFile() {
        return file;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    @Override
    public int compareTo(Result o) {
        int result = getNormalizedPath().compareTo(o.getNormalizedPath());
        if (result == 0) {
            HashCode hashCode = fold(null);
            HashCode otherHashCode = o.fold(null);
            result = HashUtil.compareHashCodes(hashCode, otherHashCode);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return compareTo((Result) o) == 0;
    }

    @Override
    public int hashCode() {
        return getNormalizedPath().hashCode();
    }
}
