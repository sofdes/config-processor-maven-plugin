/*
 * Copyright 2014 Software Design Studio Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ariht.maven.plugins.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Reads directory recursively to put collated file information such as
 * relative paths, name (without extension) and a reference to the {@link File} itself.
 * <p/>
 *
 * @author <a href="mailto:david.green@softwaredesignstudio.co.uk">David Green</a>
 */
public class DirectoryReader {

    final Log log;

    public DirectoryReader(final Log log) {
        this.log = log;
    }
    /**
     * Read directory creating FileInfo for each file found, include sub-directories.
     */
    public List<FileInfo> readFiles(final String path) throws IOException, InstantiationException, IllegalAccessException {
        log.info("Scanning directory: " + path);
        final File directory = new File(path);
        final Collection<File> allFiles = getAllFiles(directory);
        final List<FileInfo> allFilesInfo = new ArrayList<FileInfo>(allFiles.size());
        final String canonicalBaseDirectory = directory.getCanonicalPath();
        for (final File file : allFiles) {
            final FileInfo fileInfo = new FileInfo(file);

            // Remove base directory to derive sub-directory
            final String canonicalFilePath = FilenameUtils.getFullPathNoEndSeparator(file.getCanonicalPath());
            final String subDirectory = FilenameUtils.normalize(StringUtils.replaceOnce(canonicalFilePath, canonicalBaseDirectory, "") + "/");

            //fileInfo.setBaseDirectory(canonicalBaseDirectory);
            fileInfo.setRelativeSubDirectory(subDirectory);
            allFilesInfo.add(fileInfo);
        }
        return allFilesInfo;
    }

    @SuppressWarnings("rawtypes")
    private Collection<File> getAllFiles(final File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Directory not found: " + String.valueOf(directory));
        }
        final Collection allFiles = FileUtils.listFiles(directory, TrueFileFilter.TRUE, DirectoryFileFilter.DIRECTORY);
        final Collection<File> files = new ArrayList<File>(allFiles.size()); 
        for (final Object f : allFiles) {
            if (f == null) {
                continue;
            }
            log.debug("Adding file: " + f.toString());
            files.add((File) f);
        }
        return files;
    }

}
