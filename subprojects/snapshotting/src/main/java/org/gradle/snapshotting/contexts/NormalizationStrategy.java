package org.gradle.snapshotting.contexts;

import org.gradle.snapshotting.files.Fileish;

import java.nio.file.Paths;

public interface NormalizationStrategy {
    NormalizationStrategy ABSOLUTE = new NormalizationStrategy() {
        @Override
        public String normalize(Fileish file) {
            return file.getPath();
        }
    };
    NormalizationStrategy RELATIVE = new NormalizationStrategy() {
        @Override
        public String normalize(Fileish file) {
            return file.getRelativePath();
        }
    };
    NormalizationStrategy NAME_ONLY =  new NormalizationStrategy() {
        @Override
        public String normalize(Fileish file) {
            return Paths.get(file.getRelativePath()).getFileName().toString();
        }
    };
    NormalizationStrategy NONE = new NormalizationStrategy() {
        @Override
        public String normalize(Fileish file) {
            return "";
        }
    };

    String normalize(Fileish file);
}
