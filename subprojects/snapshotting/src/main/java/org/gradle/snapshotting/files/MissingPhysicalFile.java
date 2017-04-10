package org.gradle.snapshotting.files;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.io.File;

public class MissingPhysicalFile extends AbstractPhysical {
    public static final HashCode HASH = Hashing.md5().hashString("MISSING_FILE", Charsets.UTF_8);

    public MissingPhysicalFile(String path, PhysicalDirectory parent, File file) {
        super(path, parent, file);
    }
}
