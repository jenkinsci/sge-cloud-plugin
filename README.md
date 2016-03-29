This Jenkins plugin submits batch jobs to the Sun Grid Engine (SGE) batch system.  Both the open source version of SGE and the commercial [Univa](http://http://www.univa.com/) Grid Engine (UGE) are supported.

# Features

This plugin adds a new type of build step *Run job on SGE* that submits batch jobs to SGE. The build step monitors the job status and periodically appends the progress to the build's *Console Output*. Should the build fail, errors and the exit status of the job also appear. If the job is terminated in Jenkins, it is also terminated in SGE.

Builds are submitted to SGE by a new type of cloud, *SGE Cloud*.  The cloud is given a label like any other slave.  When a job with a matching label is run, *SGE Cloud* submits the build to SGE.

# Further Documentation

The Jenkins wiki contains the [main documentation for the SGE Cloud Plugin](https://wiki.jenkins-ci.org/display/JENKINS/SGE+Cloud+Plugin).

The SGE Cloud Plugin wiki here on GitHub contains an [explanation of some exit statuses](https://github.com/wavecomp/sge-cloud-plugin/wiki/Job-Exit-Status).

# Project History

`sge-cloud-plugin` was forked from [lsf-cloud-plugin](https://github.com/jenkinsci/lsf-cloud-plugin) and modified to work with SGE instead of LSF.

`sge-cloud-plugin` is currently being used in industrial production on Wave Computing's Grid Engine compute farm.  It is actively maintained.

While it might be nice to integrate `sge-cloud-plugin` and `lsf-cloud-plugin` into a single Jenkins plugin, this would be difficult to test, as few organizations have all batch systems installed.  For the sake of testability, it would probably be better to build multiple independent plugins from shared code.
