/*
 * The MIT License
 *
 * Copyright 2015 Laisvydas Skurevicius.
 * Copyright 2015 Wave Semiconductor
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
package org.jenkinsci.plugins.lsf;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jenkins.util.BuildListenerAdapter;
import org.apache.commons.io.FileUtils;

/**
 * Interface to the Sun Grid Engine (SGE) batch system
 * 
 * @author John McGehee
 * 
 * Based on the LSF version by Laisvydas Skurevicius
 */
public class LSF extends BatchSystem {

    private static final BuildListenerAdapter fakeListener
            = new BuildListenerAdapter(TaskListener.NULL);

    public LSF(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener, String COMMUNICATION_FILE, 
            String masterWorkingDirectory) {
        super(build, launcher, listener, COMMUNICATION_FILE, 
                masterWorkingDirectory);
    }
    
    private static final String OUTPUT_FILE = "sge.log";
    
    // When the Jenkins SGE plugin fails to even submit the job to SGE, it gives
    // status "J", which is not a real SGE status. 
    private static final String JENKINS_FAIL_STATUS = "J";

    // When a job completes successfully, it exits with status "0". 
    private static final String DONE_STATUS = "0";

    /** Submit the job to SGE using the qsub command.
     * 
     * Redefine the Jenkins environment variable `JOB_NAME` as `JENKINS_JOB_ID`
     * because SGE overwrites it with its own `JOB_NAME`.
     */
    @Override
    public String submitJob(String jobFileName, boolean sendEmail,
            String queueType) throws InterruptedException, IOException {

        // Inhibit email if requested
        String emailOption = "";
        if (!sendEmail) {
            emailOption = "-m n";
        }
        // submit the job to SGE
        Shell shell = new Shell("#!/bin/bash +x\n" +
                "if [ ! -x \"$SGE_BIN/qsub\" ]; then\n" +
        		"    echo \"ERROR: SGE Plugin setup: Directory " +
                "SGE_BIN='$SGE_BIN' does not contain executable SGE commands.\" 1>&2\n" +
                "    exit 1\n" +
        		"fi\n" +
                // Save Jenkins' JOB_NAME because SGE will overwrite it.
                "export JENKINS_JOB_NAME=\"$JOB_NAME\"\n" + 
        		// "env | sort\n" +
        		"rm -f " + OUTPUT_FILE + "\n" +
                "\"$SGE_BIN/qsub\" " +
                    emailOption +
                    " -q " + queueType +
                    " -N ${JOB_NAME//\\//.}" +
                    " -cwd" +
                    " -V" +
                    " -o " + OUTPUT_FILE +
                    " -j yes " + // Send stderr + stdout to OUTPUT_FILE
                    jobFileName +
                    "  &> " + COMMUNICATION_FILE);
        shell.perform(build, launcher, listener);

        // Extract the job id
        copyFileToMaster.perform(build, launcher, fakeListener);
        BufferedReader fileReader = new BufferedReader(
                new FileReader(masterWorkingDirectory + COMMUNICATION_FILE));
        // qsub prints, 'Your job jobId ("jobName") has been submitted'
        String line = fileReader.readLine();
        String jobId = "0";
        if (line == null) {
            listener.getLogger().println(
            		"ERROR: SGE qsub job submission failed.  There was no " +
                    "diagnostic message.");
        } else if (!line.startsWith("Your job ") ||
        		!line.endsWith(" has been submitted")) {
        	listener.getLogger().println(
        			"ERROR: SGE qsub job submission failed because '" +
        	        line + "'.");
        } else {
            jobId = line.trim().split(" ")[2];
            listener.getLogger().println(line);
        }
        return jobId;
    }

    @Override
    public String getJobStatus(String jobId)
            throws IOException, InterruptedException {
        // submitJob() returns "0" when it fails to even submit to SGE
    	if (jobId.equals("0")) {
    		return JENKINS_FAIL_STATUS;
    	}
    	
    	// Try to get the unfinished job status using qstat
    	String jobStatus = DONE_STATUS;
        Shell shell = new Shell("#!/bin/bash +x\n" +
        		"\"$SGE_BIN/qstat\" -u $USER > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, fakeListener);
        copyFileToMaster.perform(build, launcher, fakeListener);
        String jobStats = getValueFromFile(jobId);
        if (jobStats != null) {
        	String word[] = jobStats.trim().split("\\s+");
        	if (word.length >= 4) {
                return word[3];
        	}
        }
        listener.getLogger().println(
        		"SGE qstat says that the job is no longer running.");
        
    	// qstat did not list jobId, so the job must be finished.  Get its
    	// return status using qacct.
        String exitStatus = getFinishedJobExitStatus(jobId);
    	return exitStatus;
    }

    private String getFinishedJobExitStatus(String jobId)
            throws IOException, InterruptedException {
        Shell shell = new Shell("#!/bin/bash +x\n" +
        		"\"$SGE_BIN/qacct\" -j " + jobId + " > " + COMMUNICATION_FILE);
        String exitStatus = null;
        
        for (int i = 0; i < 10; i++) {
        	shell.perform(build, launcher, fakeListener);
        	copyFileToMaster.perform(build, launcher, fakeListener);
    		exitStatus = getValueFromFile("exit_status");
    		if (exitStatus != null) {
    			break;
    		}
    		listener.getLogger().println(
	            		"SGE qacct did not list the finished job.  Try again.");
    		Thread.sleep(5000);
        }
    	
    	if (exitStatus == null) {
        	listener.getLogger().println("SGE qacct failed to get the " +
        			"exit status for job '" + jobId + "'.  Assuming '" +
        			DONE_STATUS + "', which means the job succeeded.");
        	exitStatus = DONE_STATUS;
    	}

    	return exitStatus;
    }
    
    /** Presuming that COMMUNICATION_FILE already contains the result of
     * `qacct`or `qstat`, open it and return the value for the specified
     * key.  The file is presumed to be in the format::
     * 
     *      key1     value1
     *      key2     value    containing    white  space
     *      key3
     *          ...
     *
     * Repeated white space in the value will be normalized to a single space.
     * For example, `getValueFromFile("key2")` will return "value containing
     * white space".
     * 
     * If there is no value, such as in the case of `getValueFromFile("key3")`,
     * return `null`.
     * 
     * If `key` is not found, return `null`.
     */
    private String getValueFromFile(String key)
            throws IOException, InterruptedException {
        BufferedReader fileReader = new BufferedReader(
                new FileReader(masterWorkingDirectory + COMMUNICATION_FILE));
        String value = null;
        String line;
        while ((line = fileReader.readLine()) != null) {
        	// listener.getLogger().println("LINE: " + line);
        	String word[] = line.trim().split("\\s+");
        	if (word.length >= 2 && word[0].equals(key)) {
        		value = word[1];
        		for (int i = 2; i < word.length; i++) {
        			value += " ";
        			value += word[i];
        		}
        		break; 
        	}
        }
        fileReader.close();
    	// listener.getLogger().println("VALUE: " + value);
        return value;
    }

    @Override
    public void killJob(String jobId) throws InterruptedException {
        Shell shell = new Shell("#!/bin/bash +x\n" +
        		"\"$SGE_BIN/qdel\" " + jobId);
        shell.perform(build, launcher, listener);
    }

    @Override
    public void processStatus(String jobStatus) {
    	/**
    	 * Print an explanation for the given job status.
		 * 
		 * These explanations of status come from the qstat man page.
    	 */
        if (jobStatus.equals("d")) {
            listener.getLogger().println("A qdel command has been used to "
            		+ "initiate deletion of this job.");
        } else if (jobStatus.equals("t")) {
            listener.getLogger().println("This job is transferring and about "
            		+ "to be executed.");
        } else if (jobStatus.equals("r")) {
            listener.getLogger().println("This job is running.");
        } else if (jobStatus.equals("s")) {
            listener.getLogger().println("This job was suspended by the user "
            		+ "via the qmod command.");
        } else if (jobStatus.equals("S")) {
            listener.getLogger().println("The queue containing this job is "
            		+ "suspended and therefore this job is also suspended.");
        } else if (jobStatus.equals("T")) {
            listener.getLogger().println("This job was suspended because at "
            		+ "least one suspend threshold of its queue has been "
            		+ "exceeded.");
        } else if (jobStatus.equals("R")) {
            listener.getLogger().println("This job has been restarted.  "
            		+ "This can be caused by a job migration or because of one "
            		+ "of the reasons described in the -r section of the qsub "
            		+ "command.");
        } else if (jobStatus.equals("w")) {
            listener.getLogger().println("This job is waiting pending execution.");
        } else if (jobStatus.equals("h")) {
            listener.getLogger().println("This job is not eligible for "
            		+ "execution because it has been assigned a hold state "
            		+ "via qhold, qalter or the qsub -h option "
            		+ "or the job is waiting for completion of the jobs "
            		+ "to which job dependencies have been assigned "
            		+ "via the -hold_jid or -hold_jid-ad options of "
            		+ "qsub or qalter.");
        } else if (jobStatus.equals("E")) {
            listener.getLogger().println("This job could not be started due to "
            		+ "job properties.  The reason for the error is shown by "
            		+ "the qstat -j job_list option.");
        // These additional states are unique to the Jenkins SGE plugin.
        } else if (jobStatus.equals(JENKINS_FAIL_STATUS)) {
            listener.getLogger().println("The Jenkins SGE plugin failed to even "
            		+ "submit this job to SGE.");
        } else if (jobStatus.equals(DONE_STATUS)) {
            listener.getLogger().println("This job has completed successfully.");
        } else if (jobExitedWithErrors(jobStatus)) {
            listener.getLogger().println("The job failed with exit status '"
                    + jobStatus + "'.");
        } else {
            listener.getLogger().println("Job status '" + jobStatus
            		+ "' is not recognized.");
        }
    }

    @Override
    public void printErrorLog() throws InterruptedException {
        listener.getLogger().println("Job exited with following errors:");
        Shell shell = new Shell("#!/bin/bash +x\n cat " + COMMUNICATION_FILE);
        shell.perform(build, launcher, listener);
    }

    @Override
    public void printExitCode(String jobId)
            throws InterruptedException, IOException {
        String exitStatus = getFinishedJobExitStatus(jobId);
        if (exitStatus != null) {
        	listener.getLogger().println("Exited with exit status " + exitStatus);
        } else {
        	listener.getLogger().println(
        			"Exited, but the exit status could not be found");
        }
    }

    @Override
    public void createJobProgressFile(String jobId, String outputFileName)
            throws InterruptedException, IOException {
        Shell shell = new Shell("#!/bin/bash +x\n" +
                                "cp " + OUTPUT_FILE + " " + outputFileName);
        shell.perform(build, launcher, listener);
    }

    /**
     * While the job is still running, create a file on the slave that begins
     * after the specified offset and ending with line numberOfLines
     * (created by createJobProgressFile method)
     *
     * @param outputFileName the output file to be formatted
     * @param offset number of lines that should be skipped at the beginning
     * @param numberOfLines the last line in outputFileName to be output
     * @throws InterruptedException
     */
    @Override
    public void createFormattedRunningJobOutputFile(String outputFileName,
            int offset, int numberOfLines)
            throws InterruptedException, IOException {
        Shell shell = new Shell("#!/bin/bash +x\n tail -n+" + offset + " "
                + outputFileName + " | head -n " + (numberOfLines - offset)
                + " > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, fakeListener);
    }

    /**
     * After the job is finished, create a job output file on the slave
     * beginning with the line after the offset through the end of the file.
     *
     * @param jobId the identifier of the job
     * @param offset number of lines at the beginning of the file that should
     * be skipped
     * @throws InterruptedException
     */
    @Override
    public void createFinishedJobOutputFile(String jobId, int offset)
            throws InterruptedException {
        // because of the running job output headers
        Shell shell = new Shell("#!/bin/bash +x\n tail -n+" + offset
                + " " + OUTPUT_FILE + " > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, listener);
    }

    @Override
    public void cleanUpFiles(String jobId) throws InterruptedException {
        Shell shell = new Shell("rm -rf JOB-*-*-*-*-*");
        shell.perform(build, launcher, fakeListener);
    }

    @Override
    public boolean isRunningStatus(String jobStatus) {
        return jobStatus.equals("r");
    }

    @Override
    public boolean isEndStatus(String jobStatus) {
        return jobCompletedSuccessfully(jobStatus) ||
        		jobExitedWithErrors(jobStatus);
    }

    @Override
    public boolean jobExitedWithErrors(String jobStatus) {
        return  jobStatus.equals("E") ||
        		jobStatus.equals(JENKINS_FAIL_STATUS) ||
        		jobExitedWithNonzeroStatus(jobStatus);
    }

    private boolean jobExitedWithNonzeroStatus(String jobStatus) {
    	return !jobStatus.equals("0") && jobStatus.matches("\\d+");
	}

    @Override
    public boolean jobCompletedSuccessfully(String jobStatus) {
        return jobStatus.equals(DONE_STATUS);
    }

}
