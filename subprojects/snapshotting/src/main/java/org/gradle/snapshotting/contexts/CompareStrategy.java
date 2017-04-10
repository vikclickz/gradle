package org.gradle.snapshotting.contexts;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface CompareStrategy {
    CompareStrategy ORDER_SENSITIVE = new CompareStrategy() {
        @Override
        public Collection<Result> sort(Collection<Result> results) {
            return results;
        }
    };
    CompareStrategy ORDER_INSENSITIVE = new CompareStrategy() {
        @Override
        public Collection<Result> sort(Collection<Result> results) {
            List<Result> sortedResults = Lists.newArrayList(results);
            Collections.sort(sortedResults);
            return sortedResults;
        }
    };

    Collection<Result> sort(Collection<Result> results);
}
