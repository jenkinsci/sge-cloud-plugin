This Jenkins plugin submits batch jobs to the Sun Grid Engine (SGE) batch system.  Both the open source version of SGE and the commercial [Univa](http://http://www.univa.com/) Grid Engine (UGE) are supported.

# Features

This plugin adds a new type of build step *Run job on SGE* that submits batch jobs to SGE. The build step monitors the job status and periodically appends the progress to the build's *Console Output*. Should the build fail, errors and the exit status of the job also appear. If the job is terminated in Jenkins, it is also terminated in SGE.

Builds are submitted to SGE by a new type of cloud, *SGE Cloud*.  The cloud is given a label like any other agent.  When a job with a matching label is run, *SGE Cloud* submits the build to SGE.

# Documentation

The Jenkins wiki contains the [main documentation for the SGE Cloud Plugin](https://plugins.jenkins.io/sge-cloud-plugin).

# Project History
## LSF Cloud Plugin

`sge-cloud-plugin` was forked from [lsf-cloud-plugin](https://github.com/jenkinsci/lsf-cloud-plugin) and modified to work with SGE.

## SGE Cloud Plugin

`sge-cloud-plugin` is currently being used in industrial production on Wave Computing's Grid Engine compute farm.  It is actively maintained.

While it might be nice to integrate `sge-cloud-plugin` and `lsf-cloud-plugin` into a single Jenkins plugin, this would be difficult to test, as few organizations have all batch systems installed.

## Condor Cloud Plugin (future)

From time to time people inquire about a Condor version of this plugin. To create this you would fork the SGE plugin, then replace the SGE commands it sends with Condor commands.  No official Jenkins Condor Plugin has materialized, but potential candidates do turn up in a search of GitHub. Good luck.
