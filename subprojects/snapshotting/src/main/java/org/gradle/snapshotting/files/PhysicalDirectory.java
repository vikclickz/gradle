package org.gradle.snapshotting.files;

import java.io.File;

public class PhysicalDirectory extends AbstractPhysical implements Physical, Directoryish {
    public PhysicalDirectory(String path, PhysicalDirectory parent, File file) {
        super(path, parent, file);
    }
}
