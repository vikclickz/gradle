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

import com.google.common.collect.Maps;

import java.util.Map;

public class FileCollectionSnapshotCollector implements NormalizedSnapshotCollector {
    private final TaskFilePropertyCompareStrategy compareStrategy;
    private final SnapshotNormalizationStrategy normalizationStrategy;
    Map<String, NormalizedFileSnapshot> snapshots = Maps.newLinkedHashMap();

    public FileCollectionSnapshotCollector(SnapshotNormalizationStrategy normalizationStrategy, TaskFilePropertyCompareStrategy compareStrategy) {
        this.compareStrategy = compareStrategy;
        this.normalizationStrategy = normalizationStrategy;
    }

    @Override
    public void collectSnapshot(NormalizedFileSnapshot normalizedSnapshot) {
        String absolutePath = normalizedSnapshot.getPath();
        if (!snapshots.containsKey(absolutePath)) {
            snapshots.put(absolutePath, normalizedSnapshot);
        }
    }

    public FileCollectionSnapshot finish() {
        return new DefaultFileCollectionSnapshot(snapshots, compareStrategy, normalizationStrategy.isPathAbsolute());
    }
}
