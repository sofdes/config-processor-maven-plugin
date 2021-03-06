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
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.plugin.logging.Log;

/**
 * Reads directory recursively to put collated file information such as
 * relative paths, name (without extension) and a reference to the {@link File} itself.
 */
public class DirectoryReader {

    private final Log log;
    private final String pathSeparator;
    private final List<File> filesToIgnore;

    private static final List<File> EMPTY_FILE_LIST = Collections.unmodifiableList(new LinkedList<File>());

    public DirectoryReader(final Log log, final String pathSeparator, final List<String> filenamesToIgnore) {
        this.log = log;
        this.pathSeparator = pathSeparator;
        this.filesToIgnore = processFilesToIgnore(filenamesToIgnore);
    }

    private boolean isFileToIgnore(File file) {
        for (File f : filesToIgnore) {
            if (StringUtils.startsWith(file.getAbsolutePath(), f.getAbsolutePath())) {
                log.debug("Matched prefix so will ignore: \n" + file.getAbsolutePath() + "\n" + f.getAbsolutePath());
                return true;
            } else {
                log.debug("Not same: \n" + file.getAbsolutePath() + "\n" + f.getAbsolutePath());
            }
        }
        return false;
    }

    /**
     * Read directory creating FileInfo for each file found, include sub-directories.
     */
    public List<FileInfo> readFiles(final String path) throws IOException, InstantiationException, IllegalAccessException {
        log.debug("Scanning directory: " + path);
        final File directory = new File(path);
        final Collection<File> allFiles = getAllFiles(directory);
        if (allFiles.isEmpty()) {
            log.warn("No files found in directory: " + path);
        }
        final List<FileInfo> allFilesInfo = new ArrayList<FileInfo>(allFiles.size());
        final String canonicalBaseDirectory = directory.getCanonicalPath();
        for (final File file : allFiles) {
            final FileInfo fileInfo = new FileInfo(file);
            // Remove base directory to derive sub-directory
            final String canonicalFilePath = FilenameUtils.getFullPathNoEndSeparator(file.getCanonicalPath());
            final String subDirectory = FilenameUtils.normalize(StringUtils.replaceOnce(canonicalFilePath, canonicalBaseDirectory, "") + pathSeparator);
            fileInfo.setRelativeSubDirectory(subDirectory);
            allFilesInfo.add(fileInfo);
        }
        return allFilesInfo;
    }

    @SuppressWarnings("rawtypes")
    private Collection<File> getAllFiles(final File directory) {
        if (!directory.exists()) {
            log.warn("Directory does not exist: " + directory.getPath());
            return EMPTY_FILE_LIST;
        }
        final Collection allFiles = FileUtils.listFiles(directory, TrueFileFilter.TRUE, DirectoryFileFilter.DIRECTORY);
        final Collection<File> files = new ArrayList<File>(allFiles.size());
        for (final Object o : allFiles) {
            if (o != null && o instanceof File) {
                final File file = (File) o;
                if (isFileToIgnore(file)) {
                    log.info("Ignoring: " + file.toString());
                } else {
                    log.debug("Adding file: " + file.toString());
                    files.add(file);
                }
            } else {
                log.warn("Not a file: " + ToStringBuilder.reflectionToString(o));
            }
        }
        return files;
    }

    private List<File> processFilesToIgnore(final List<String> filesToIgnore) {
        if (filesToIgnore == null || filesToIgnore.isEmpty()) {
            return EMPTY_FILE_LIST;
        }
        final List<File> filesIgnored = new ArrayList<File>(filesToIgnore.size());
        for (String fileToIgnore : new LinkedHashSet<String>(filesToIgnore)) {
            if (StringUtils.isNotBlank(fileToIgnore)) {
                fileToIgnore = FilenameUtils.separatorsToSystem(FilenameUtils.normalize(fileToIgnore.trim()));
                final File file = new File(fileToIgnore);
                if (file.exists()) {
                    log.debug("Adding ignore for file: " + file.getAbsolutePath());
                    filesIgnored.add(file);
                }
            }
        }
        return filesIgnored;
    }
}
