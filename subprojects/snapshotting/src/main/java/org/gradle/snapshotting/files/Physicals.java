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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.gradle.api.UncheckedIOException;

import java.io.File;
import java.io.IOException;

public final class Physicals {
    public static Physical of(String path, PhysicalDirectory parent, File file) {
        try {
            if (!file.exists()) {
                return new MissingPhysicalFile(path, parent, file);
            } else if (file.isDirectory()) {
                return new PhysicalDirectory(path, parent, file);
            } else {
                HashCode contentHash = Files.hash(file, Hashing.md5());
                return new PhysicalFile(path, parent, file, contentHash);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
