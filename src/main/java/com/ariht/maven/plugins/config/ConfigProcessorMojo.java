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
 * For generating config files, shell and other scripts or any environment specific text files
 * that have different configuration for different environments.
 * <p/>
 * Files in the 'deploy' directory are a special case as they will be copied to the base of 
 * each environment so they can be used to call the functional scripts in sub-directories.
 *
 * <p/><b>Example Inputs:</b>
 *
 * <pre>
 *   .../templates/deploy/your_deploy_script.sh
 *   .../templates/jboss/do_something.cli
 *   .../templates/properties/web-app.properties
 *   .../templates/liquibase/liquibase.properties
 *
 *   .../filters/env-a.filter
 *   .../filters/env-b.filter
 *   .../filters/env-c.filter
 * </pre>
 *
 * <p/><b>Output:</b>
 *
 * <pre>
 *   .../env-a/your_deploy_script.sh
 *   .../env-a/jboss/do_something.cli
 *   .../env-a/properties/web-app.properties
 *   .../env-a/liquibase/liquibase.properties
 *
 *   .../env-b/your_deploy_script.sh
 *   .../env-b/jboss/do_something.cli
 *   .../env-b/properties/web-app.properties
 *   .../env-b/liquibase/liquibase.properties
 *
 *   .../env-c/your_deploy_script.sh
 *   .../env-c/jboss/do_something.cli
 *   .../env-c/properties/web-app.properties
 *   .../env-c/liquibase/liquibase.properties
 * </pre>
 *
 * @author <a href="mailto:david.green@softwaredesignstudio.co.uk">David Green</a>
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDirectInvocation = false)
public class  ConfigProcessorMojo extends AbstractMojo {

    @Parameter (defaultValue = CharEncoding.UTF_8)
    protected String encoding;
    @Parameter (defaultValue = "deploy")
    protected String deployTemplatesDirectory;
    @Parameter (defaultValue = "${basedir}/src/config/templates")
    protected String templatesBasePath;
    @Parameter (defaultValue = "${basedir}/src/config/filters")
    protected String filtersBasePath;
    @Parameter (defaultValue = "${basedir}/target/generated-config")
    protected String outputBasePath;

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
        getLog().info("Scanning filters directory: " + filtersBasePath);
        final DirectoryReader filtersDirectoryReader = new DirectoryReader();
        final List<FileInfo> filters = filtersDirectoryReader.readFiles(filtersBasePath);

        getLog().info("Scanning templates directory: " + templatesBasePath);
        final DirectoryReader templatesDirectoryReader = new DirectoryReader(deployTemplatesDirectory, true);
        final List<FileInfo> templates = templatesDirectoryReader.readFiles(templatesBasePath);

        getLog().info("Generating: " + outputBasePath);
        for (final FileInfo filter : filters) {
            for (final FileInfo template : templates) {
                generateConfig(template, filter, outputBasePath);
            }
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
        final String rawTemplate = FileUtils.readFileToString(template.getFile());
        getLog().debug("Applying filter : " + filter.toString() + " to template : " + template.toString());
        final Properties properties = readFilterIntoProperties(filter);
        final String processedTemplate = StrSubstitutor.replace(rawTemplate, properties);
        FileUtils.writeStringToFile(new File(outputFilename), processedTemplate, encoding);
    }

    /**
     * Filter files contain the properties we wish to substitute in templates.
     * Use Apache Commons Configuration to load these.
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
            getLog().debug("Create : " + outputDir);
            FileUtils.forceMkdir(outputDir);
        }
        return FilenameUtils.normalize(outputDirectory);
    }

    /**
     * Concatenate together the filter's directory with the template's - 'deploy' templates just go into the 
     * base path so only have the filter (i.e. the environment they are intended for).
     */
    private String getOutputPath(final FileInfo template, final FileInfo filter, final String outputBasePath) {
        final StringBuilder sb = new StringBuilder(outputBasePath + "/");
        sb.append(filter.getRelativeSubDirectory()).append(filter.getNameWithoutExtension()).append("/");
        if (!template.isDeployDirectory()) {
            sb.append(template.getRelativeSubDirectory()).append("/");
        }
        return FilenameUtils.normalize(sb.toString());
    }

    private void deleteOutputDirectory() throws IOException {
        getLog().debug("Deleting output directory " + String.valueOf(outputBasePath));
        final File outputDir = new File(outputBasePath);
        if (outputDir.exists()) {
            getLog().debug("Delete : " + outputDir);
            FileUtils.forceDelete(outputDir);
        }
    }

}
