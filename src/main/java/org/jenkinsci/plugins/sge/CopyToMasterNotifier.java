/*
 * The MIT License
 *
 * Copyright (c) 2009-2011, Manufacture Française des Pneumatiques Michelin, Romain Seguy
 * Copyright (c) 2019, Wave Computing, Inc. John McGehee
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.sge;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson.MasterComputer;
import hudson.model.TopLevelItem;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class is forked from the Jenkins Copy To Slave plugin. The Copy To Slave plugin is deprecated because it can
 * copy arbitrary files.
 *
 * @author Romain Seguy (http://openromain.blogspot.com)
 * @author John McGehee (http://johnnado.com)
 */
public class CopyToMasterNotifier extends Notifier {

    private final String includes;
    private static final String excludes = "";
    private static final boolean overrideDestinationFolder = true;
    private final String destinationFolder; //masterWorkingDirectory
    private static final boolean runAfterResultFinalised = true;
    private final static Logger LOGGER = Logger.getLogger(CopyToMasterNotifier.class.getName());

    @DataBoundConstructor
    public CopyToMasterNotifier(String includes, String destinationFolder) {
        this.includes = includes;
        this.destinationFolder = destinationFolder;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return getRunAfterResultFinalised();
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        if(Computer.currentComputer() instanceof SlaveComputer) {
            FilePath destinationFilePath;
            if(isOverrideDestinationFolder() && StringUtils.isNotBlank(getDestinationFolder())) {
                destinationFilePath = new FilePath(new File(env.expand(getDestinationFolder())));
            }
            else {
                destinationFilePath = getProjectWorkspaceOnMaster(build, listener.getLogger());
            }

            FilePath projectWorkspaceOnSlave = build.getProject().getWorkspace();

            String includes = env.expand(getIncludes());
            String excludes = env.expand(getExcludes());

            listener.getLogger().printf("[copy-to-slave] Copying '%s', excluding %s, from '%s' on '%s' to '%s' on the master.\n",
                    includes, StringUtils.isBlank(excludes) ? "nothing" : '\'' + excludes + '\'', projectWorkspaceOnSlave.toURI(),
                    Computer.currentComputer().getNode(), destinationFilePath.toURI());

            projectWorkspaceOnSlave.copyRecursiveTo(includes, excludes, destinationFilePath);
        }
        else if(Computer.currentComputer() instanceof MasterComputer) {
            listener.getLogger().println(
                    "[copy-to-slave] The build is taking place on the master node, no copy back to the master will take place.");
        }

        return true;
    }

    public static FilePath getProjectWorkspaceOnMaster(AbstractBuild build, PrintStream logger) {
        return getProjectWorkspaceOnMaster(build, build.getProject(), logger);
    }

    private static FilePath getProjectWorkspaceOnMaster(AbstractBuild build, AbstractProject project, PrintStream logger) {
        FilePath projectWorkspaceOnMaster;

        // free-style projects
        if(project instanceof FreeStyleProject) {
            FreeStyleProject freeStyleProject = (FreeStyleProject) project;

            // do we use a custom workspace?
            if(freeStyleProject.getCustomWorkspace() != null && freeStyleProject.getCustomWorkspace().length() > 0) {
                projectWorkspaceOnMaster = new FilePath(new File(freeStyleProject.getCustomWorkspace()));
            }
            else {
                projectWorkspaceOnMaster = Jenkins.getInstance().getWorkspaceFor(freeStyleProject);
            }
        }
        else {
            // Quote separator, as String.split() takes a regex and
            // File.separator isn't a valid regex character on Windows
            final String separator = Pattern.quote(File.separator);
            String pathOnMaster = Jenkins.getInstance().getWorkspaceFor((TopLevelItem)project.getRootProject()).getRemote();
            String parts[] = build.getWorkspace().getRemote().
                    split("workspace" + separator + project.getRootProject().getName());
            if (parts.length > 1) {
                // This happens for the non free style projects (like multi configuration projects, etc)
                // So we'll just add the extra part of the path to the workspace
                pathOnMaster += parts[1];
            }
            projectWorkspaceOnMaster = new FilePath(new File(pathOnMaster));
        }

        try {
            // create the workspace if it doesn't exist yet
            projectWorkspaceOnMaster.mkdirs();
        }
        catch (Exception e) {
            if(logger != null) {
                logger.println("An exception occured while creating " + projectWorkspaceOnMaster.getName() + ": " + e);
            }
            LOGGER.log(Level.SEVERE, "An exception occured while creating " + projectWorkspaceOnMaster.getName(), e);
        }

        return projectWorkspaceOnMaster;
    }


    public String getIncludes() {
        return includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public boolean isOverrideDestinationFolder() {
        return overrideDestinationFolder;
    }
    
    public boolean getRunAfterResultFinalised() {
        return runAfterResultFinalised;
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(CopyToMasterNotifier.class);
        }

        @Override
        public String getDisplayName() {
            return new Localizable(ResourceBundleHolder.get(CopyToMasterNotifier.class), "DisplayName").toString();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> item) {
            return true;
        }

    }

}
