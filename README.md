This Jenkins plugin submits batch jobs to the Sun Grid Engine (SGE) batch system.  Both the open source version of SGE 2011.11 and the commercial [Univa](http://http://www.univa.com/) Grid Engine (UGE) 8.3.1 are supported.

# Features

This plugin adds a new type of build step *Run job on SGE* that submits batch jobs to SGE. The build step monitors the job status and periodically (default one minute) appends the progress to the build's *Console Output*. Should the build fail, errors and the exit status of the job also appear. If the job is terminated in Jenkins, it is also terminated in SGE.

Builds are submitted to SGE by a new type of cloud, *SGE Cloud*.  The cloud is given a label like any other slave.  When a job with a matching label is run, *SGE Cloud* submits the build to SGE.

Files can be uploaded and sent to SGE before the execution of the job and downloaded from SGE after the job finishes.  	Currently this feature only supports shared file systems.

The job owner can select whether SGE should send an email when the job finishes.

# Installation

Install the prerequisite plugins:

* `SSH Slaves` plugin version 1.9 or greater
* `Copy to Slave` plugin version 1.4.3 or greater

`sge-cloud-plugin` is not an official Jenkins plugin, so you must compile and load it yourself.  After you clone the git repository, build it using Maven:

    cd sge-cloud-plugin/
    mvn install     # Sometimes 'mvn clean install' works better

In *Manage Jenkins > Plugin Manager*, select the *Advanced* tab.  Use *Upload Plugin* to upload the plugin file `sge-cloud-plugin/target/sge-cloud.hpi`to Jenkins.

# Set Up Jenkins

In SGE, add your Jenkins master host as an SGE submit host.

In *Manage Jenkins > Configure System*, add *Environment Variables*:

* For open source SGE:

  Name | Value
  -----|------
  `SGE_ROOT` | `/path/to/sge`
  `SGE_BIN` | `/path/to/sge/bin/linux-x64`

* For commercial UGE:

  Name | Value
  -----|------
  `SGE_ROOT` | `/path/to/uge`
  `SGE_BIN` | `/path/to/uge/bin/lx-amd64`
  `SGE_CELL` | Your cell name
  `SGE_CLUSTER_NAME` | Your cluster name
  `SGE_EXECD_PORT` | `64455`
  `SGE_QMASTER_PORT` | `64444`

There are various other [ways to add Jenkins environment variables](http://stackoverflow.com/questions/5818403/jenkins-hudson-environment-variables/), but the above is one of the most dependable.

In *Manage Jenkins > Configure System*, add a new cloud of type *SGE Cloud*.  Fill in the required information for the newly created cloud.

# Set Up a Project to run on SGE

In a project, specify the *Label* that you specified in *SGE Cloud*.

Add a *Run job on SGE* build step and specify the batch job script you want to run on SGE.

Now, when Jenkins runs the project, it will run on the *SGE Cloud* that has the matching label.

## Set Your Script to Fail on the First Failure

By default, the exit status of the last command determines the success or failure of the build step.  For example, the following script would be inappropriately considered a success:

    ls /nonexistent    # Error, exit status 2
    echo "This echo command succeeds with exit status 0"

If you prefer that your job fail and halt upon the first nonzero exit status, use the [Bash -e option](http://www.tldp.org/LDP/abs/html/options.html).  The following script will fail upon the first error:

    set -e
    ls /nonexistent    # Error, exit status 2
    echo "This is never executed because the above ls command failed."


## Additional qsub Options

So that you can see the `qsub` command that was used to submit jobs, the SGE Plugin prints the qsub command to the Jenkins job *Console Output*:

    Submitting SGE job using the command:
        "$SGE_BIN/qsub" ...    # Options not shown in docs because they will undoubtably be out-of-date

It is possible to specify additional `qsub` command line options within the *Run job on SGE* build script on lines beginning with #$. For example:

    #$ -P project_name

While this might sometimes be useful, it can cause trouble if your *Run job on SGE* build step inadvertently contains `#$`.  In particular, this can happen if you comment out a line that begins with `$`:

    #$SOME_COMMAND

There is no such `qsub` command line option `SOME_COMMAND`, so you get the unhelpful message:

    qsub: Unknown option

# Job States

## Unfinished Jobs

The [qstat man page](http://gridscheduler.sourceforge.net/htmlman/htmlman1/qstat.html) describes the following job states (job status) defined in SGE.  Each state is a string whose first character is most meaningful:

* "d", for deletion
* "E", for error
* "h", for hold
* "r", for running
* "R", for restarted
* "s", for suspended
* "S", for queue suspended
* "t", for transfering
* "T", for threshold
* "w", for waiting

## Finished Jobs

The above states only describe jobs that have not yet finished, while the Jenkins SGE plugin expects that completed jobs also have a state.  Therefore the SGE plugin derives the state of a finished job from its shell exit status:

* "0" (zero) for a successfully finished job
* "1" through "255" for a job that failed with a nonzero exit status

Exit status above 128 indicates that a signal terminated the job.  See the wiki for an [explanation of some exit statuses](https://github.com/jmcgeheeiv/sge-cloud-plugin/wiki/Job-Exit-Status).

Finally, when the Jenkins SGE plugin could not even submit the job to SGE, the job is given the state:

* "J", for Jenkins SGE plugin failure to submit the job

# Viewing the Job Workspace

Each project has a *Workspace* button that you can use to view the project workspace files in your web browser.  This handy feature relies on the slave that executed the job.  SGE slaves are are reused and if kept busy they can live a long and productive life.  However, slaves left idle for an extended time are deleted.  Once the slave is gone, the *Workspace* button will no longer work.  Then the files can only be viewed using other methods like the command line.

In *Jenkins > Manage Jenkins > Configure System > SGE Cloud*, the *Maximum idle time* field controls how long idle slaves are retained.  If you find that slaves disappear while you still want to view the workspace, increase  *Maximum idle time*.

# Environment Variables

Jenkins [adds environment variables to the environment](https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables), and these are imported into the SGE job environment.  Then [SGE adds some more](http://gridscheduler.sourceforge.net/htmlman/htmlman1/qsub.html).  There is just one variable name collision.  Before SGE overwrites Jenkins' `JOB_NAME`, the Jenkins value is saved in environment variable `JENKINS_JOB_NAME`.

# Project History

`sge-cloud-plugin` was forked from [lsf-cloud-plugin](https://github.com/jenkinsci/lsf-cloud-plugin) and modified to work with SGE instead of LSF.

`sge-cloud-plugin` is not yet an official Jenkins plugin, yet it is currently being used in industrial production on Wave Computing's Grid Engine compute farm.  It does work and we actively maintain it.

While it might be nice to integrate `sge-cloud-plugin` and `lsf-cloud-plugin` into a single Jenkins plugin, this would be difficult to test, as few organizations have all batch systems installed.  For the sake of testability, it would probably be better to build multiple independent plugins from shared code.
