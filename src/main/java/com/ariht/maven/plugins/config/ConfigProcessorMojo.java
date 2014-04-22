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
import java.util.List;
import java.util.Properties;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates config and scripts for multiple target environments using
 * template placeholder substitution from values in multiple filter files.
 *
 * @author <a href="mailto:david.green@softwaredesignstudio.co.uk">David Green</a>
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDirectInvocation = false)
public class  ConfigProcessorMojo extends AbstractMojo {

    @Parameter (defaultValue = CharEncoding.UTF_8)
    protected String encoding;
    @Parameter (defaultValue = "${basedir}/src/config/templates")
    protected String templatesBasePath;
    @Parameter (defaultValue = "${basedir}/src/config/filters")
    protected String filtersBasePath;
    @Parameter (defaultValue = "${basedir}/target/generated-config")
    protected String outputBasePath;

    private static final String PATH_SEPARATOR = "/";

    /**
     * For properties substituted from every filter, create config based on each template.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            deleteOutputDirectory();
            processTemplatesAndGenerateConfig();
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void processTemplatesAndGenerateConfig() throws Exception {
        final DirectoryReader directoryReader = new DirectoryReader(getLog(), PATH_SEPARATOR);
        final List<FileInfo> filters = directoryReader.readFiles(filtersBasePath);
        final List<FileInfo> templates = directoryReader.readFiles(templatesBasePath);
        getLog().debug("Outputs will go into : " + outputBasePath);
        for (final FileInfo filter : filters) {
            for (final FileInfo template : templates) {
                generateConfig(template, filter, outputBasePath);
            }
            getLog().info("");
        }
    }

    /**
     * Read properties from filter file and substitute template place-holders.
     * Write results to output path with same relative path as input filters.
     *
     * Typical output is to ...target/filter-sub-dir/template-dir/template.name
     */
    private void generateConfig(final FileInfo template, final FileInfo filter, final String outputBasePath) throws IOException, ConfigurationException {
        final String outputDirectory = createOutputDirectory(template, filter, outputBasePath);
        final String templateFilename = template.getFile().getName();
        final String outputFilename = FilenameUtils.separatorsToSystem(outputDirectory + templateFilename);
        getLog().info("Generating : " + String.valueOf(outputFilename));
        getLog().debug("Applying filter : " + filter.toString() + " to template : " + template.toString());
        final String rawTemplate = FileUtils.readFileToString(template.getFile());
        final Properties properties = readFilterIntoProperties(filter);
        final String processedTemplate = StrSubstitutor.replace(rawTemplate, properties);
        FileUtils.writeStringToFile(new File(outputFilename), processedTemplate, encoding);
    }

    /**
     * Filter files contain the properties we wish to substitute in templates.
     *
     * Uses Apache Commons Configuration to load filters.
     */
    private Properties readFilterIntoProperties(final FileInfo filter) throws ConfigurationException {
        final PropertiesConfiguration config = new PropertiesConfiguration(filter.getFile());
        config.setEncoding(encoding);
        final String filterSource = filter.getRelativeSubDirectory() + filter.getNameWithoutExtension();
        config.setProperty("filter.source", FilenameUtils.separatorsToUnix(filterSource));
        return ConfigurationConverter.getProperties(config);
    }

    /**
     * Prepare output directory: base-path/filter-sub-dir/template-dir/template.name
     */
    private String createOutputDirectory(final FileInfo template, final FileInfo filter, final String outputBasePath) throws IOException {
        final String outputDirectory = getOutputPath(template, filter, outputBasePath);
        final File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            getLog().debug("Creating : " + outputDir);
            FileUtils.forceMkdir(outputDir);
        }
        return FilenameUtils.normalize(outputDirectory);
    }

    /**
     * Concatenate together the filter's directory with the template's - 'deploy' templates just go into the 
     * base path so only have the filter (i.e. the environment they are intended for).
     */
    private String getOutputPath(final FileInfo template, final FileInfo filter, final String outputBasePath) {
        final String outputPath = outputBasePath + PATH_SEPARATOR
                                + filter.getRelativeSubDirectory()
                                + filter.getNameWithoutExtension() + PATH_SEPARATOR
                                + template.getRelativeSubDirectory() + PATH_SEPARATOR;
        return FilenameUtils.normalize(outputPath);
    }

    private void deleteOutputDirectory() throws IOException {
        final File outputDir = new File(outputBasePath);
        if (outputDir.exists()) {
            getLog().debug("Deleting : " + outputDir);
            FileUtils.forceDelete(outputDir);
        }
    }

}
