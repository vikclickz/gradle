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

package org.gradle.snapshotting.files;

import java.io.File;

public abstract class AbstractPhysical extends AbstractFileish implements Physical {
    private final File file;

    protected AbstractPhysical(String path, PhysicalDirectory parent, File file) {
        super(path, parent);
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public PhysicalDirectory getParent() {
        return (PhysicalDirectory) super.getParent();
    }

    @Override
    public String getRelativePath() {
        if (getParent() == null) {
            return getFile().getName();
        }
        return getParent().getFile().toPath().relativize(getFile().toPath()).toString();
    }
}
