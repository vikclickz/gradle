package org.gradle.snapshotting.files;

import java.io.File;

public interface Physical extends Fileish {
    File getFile();
}
