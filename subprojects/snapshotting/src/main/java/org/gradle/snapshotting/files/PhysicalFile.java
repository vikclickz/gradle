package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhysicalFile extends AbstractPhysical implements Physical, FileishWithContents {
    private final HashCode contentHash;

    public PhysicalFile(String path, PhysicalDirectory parent, File file, HashCode contentHash) {
        super(path, parent, file);
        this.contentHash = contentHash;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(getFile());
    }

    @Override
    public HashCode getContentHash() {
        return contentHash;
    }
}
