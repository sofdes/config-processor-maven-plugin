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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Container to information about a file so that the processors can input and output
 * relative paths and filenames easily as they've already been determined and placed here.
 *
 * @author <a href="mailto:david.green@softwaredesignstudio.co.uk">David Green</a>
 */
public class FileWithInfo {

    private final File file;
    private final String name;
    private String baseDirectory;
    private String subDirectory;
    private boolean isDeployDirectory = false;
    
    public FileWithInfo(final File file) {
        this.file = file;
        this.name = FilenameUtils.removeExtension(file.getName());
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(final String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public void setSubDirectory(final String relativeSubDirectory) {
        this.subDirectory = relativeSubDirectory;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public boolean isDeployDirectory() {
        return isDeployDirectory;
    }

    public void setDeployDirectory(boolean isDeployDirectory) {
        this.isDeployDirectory = isDeployDirectory;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
