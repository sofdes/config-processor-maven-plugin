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
import org.apache.commons.lang3.StringUtils;

/**
 * Reads directory recursively to put collated file information such as
 * relative paths, name (without extension) and a reference to the {@link File} itself.
 * <p/>
 *
 * Templates in the deploy directory are a special case - they
 * will be written to the <strong>base</strong> directory in order to coordinate 
 * the overall deployment process for an environment e.g.:-
 *
 * <pre>
 *   <strong>.../your_deploy_script.sh</strong>
 *
 *   .../jboss/do_something.cli
 *   .../liquibase/changelog-master.xml
 *   .../liquibase/liquibase.properties
 * </pre>
 *
 *
 * @author <a href="mailto:david.green@softwaredesignstudio.co.uk">David Green</a>
 */
public class DirectoryReader {

    private final String deployTemplatesDirectory;
    private final boolean isTemplatesDirectory;

    public DirectoryReader() {
        this(null, false);
    }

    public DirectoryReader(final boolean isTemplatesDirectory) {
        this(null, isTemplatesDirectory);
    }

    public DirectoryReader(final String deployTemplatesDirectory, final boolean isTemplatesDirectory) {
        if (StringUtils.isBlank(deployTemplatesDirectory)) {
            this.deployTemplatesDirectory = null;
        } else {            
            this.deployTemplatesDirectory = FilenameUtils.normalize("/" + FilenameUtils.separatorsToUnix(deployTemplatesDirectory) + "/");
        }
        this.isTemplatesDirectory = isTemplatesDirectory;
    }

    /**
     * Read directory creating FileInfo for each file found, include sub-directories.
     */
    public List<FileWithInfo> readFiles(final String path) throws IOException, InstantiationException, IllegalAccessException {
        final File directory = new File(path);
        final Collection<File> allFiles = getAllFiles(directory);
        final List<FileWithInfo> allFilesInfo = new ArrayList<FileWithInfo>(allFiles.size());
        final String canonicalBaseDirectory = directory.getCanonicalPath();
        for (final File file : allFiles) {
            final FileWithInfo fileInfo = new FileWithInfo(file);

            // Remove base directory to derive sub-directory
            final String canonicalFilePath = FilenameUtils.getFullPathNoEndSeparator(file.getCanonicalPath());
            final String subDirectory = FilenameUtils.normalize(StringUtils.replaceOnce(canonicalFilePath, canonicalBaseDirectory, "") + "/");

            fileInfo.setBaseDirectory(canonicalBaseDirectory);
            fileInfo.setSubDirectory(subDirectory);
            final boolean isDeployDirectory = isDeployDirectory(subDirectory);
            fileInfo.setDeployDirectory(isDeployDirectory);
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
            files.add((File) f);
        }
        return files;
    }

    private boolean isDeployDirectory(final String subDirectory) {
        return isTemplatesDirectory && StringUtils.equals(subDirectory, deployTemplatesDirectory);
    }

}
