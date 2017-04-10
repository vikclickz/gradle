package org.gradle.snapshotting.operations;

import org.gradle.snapshotting.SnapshotterState;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.PhysicalDirectory;
import org.gradle.snapshotting.files.Physicals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class ProcessDirectory extends Operation {
    private final PhysicalDirectory root;
    private Iterator<File> files;

    public ProcessDirectory(PhysicalDirectory root, Context context) {
        super(context);
        this.root = root;
    }

    @Override
    public boolean execute(SnapshotterState state, List<Operation> dependencies) throws IOException {
        File rootFile = root.getFile();
        if (files == null) {
            final List<File> allFiles = new ArrayList<>();
            Files.walkFileTree(rootFile.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    allFiles.add(file.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
            files = allFiles.iterator();
        }

        if (!files.hasNext()) {
            return true;
        }

        while (files.hasNext()) {
            File file = files.next();

            // Do not process the root file
            if (file.equals(rootFile)) {
                continue;
            }
            applyToAncestry(new StringBuilder(rootFile.getAbsolutePath()).append('/'), rootFile, file, dependencies);
            break;
        }
        return false;
    }

    private void applyToAncestry(StringBuilder path, File root, File file, List<Operation> dependencies) {
        File parent = file.getParentFile();
        if (!parent.equals(root)) {
            applyToAncestry(path, root, parent, dependencies);
            path.append('/');
        }
        path.append(file.getName());
        dependencies.add(new ApplyTo(Physicals.of(path.toString(), this.root, file)));
    }
}
