# Jenkins SGE Plugin Developer Documentation

This page is of interest to developers only.

## Building and Releasing to jenkins-ci.org

The following procedure provides additional detail to the instructions in [Hosting Plugins](https://wiki.jenkins.io/display/JENKINS/Hosting+Plugins).

### Install prerequisite packages

```
sudo apt-get install maven
sudo apt-get install default-jdk
```

### Clone the plugin source code

```
git clone git@github.com:jenkinsci/sge-cloud-plugin.git
```

### Set up authentication

This procedure presumes that you are using SSH public key encryption with GitHub.

As instructed in [Hosting Plugins](https://wiki.jenkins.io/display/JENKINS/Hosting+Plugins),
create a `~/.m2/settings.xml` file containing your `jenkins.io` credentials:
```
settings.xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
 
  <servers>
    <server>
      <id>maven.jenkins-ci.org</id> <!-- For parent 1.397 or newer; this ID is used for historical reasons and independent of the actual host name -->
      <username>...</username>
      <password>...</password>
    </server>
  </servers>
</settings>
```

### Run the build and release the plugin

```
cd sge-cloud-plugin
git clean -fdx .    # Clean from last run if necessary
mvn release:prepare release:perform
```

The build will ask you if you want to use different versions for this and the next development version.
Enter <Return> to accept the defaults:
```
What is the release version for "Jenkins SGE Plugin"? (org.jenkins-ci.plugins:sge-cloud-plugin) 1.21: : 
What is SCM release tag or label for "Jenkins SGE Plugin"? (org.jenkins-ci.plugins:sge-cloud-plugin) sge-cloud-plugin-1.21: : 
What is the new development version for "Jenkins SGE Plugin"? (org.jenkins-ci.plugins:sge-cloud-plugin) 1.22-SNAPSHOT: : 
```

## Classes in the plugin

### BatchSystem.java
This is an abstract class, all of its methods are abstract, and it represents all batch systems. It defines the interaction with the batch system methods: submit the job; kill the job; get the status of the job; get the output of a running job (and format it for clear reading); get the output of a finished job; check if a given status is an ending status, running status, job ended with errors or job ended successfully; print the error log and exit code; execute specific actions depending on the status of the job. This class must be extended by specific batch systems like `SGE` and have its methods implemented depending on the specifics of the batch system.
### SGE.java
This class extends the `BatchSystem` class and implements all of its methods. The `BatchSystem` methods are implemented using the actions and commands specific to `SGE` batch system. The interaction with `SGE` is realized through execution of shell commands and extraction of needed information from the output of the commands.
### SGESlave.java
This class represents the agent created by the cloud when a job with the appropriate label is run. It extends the `Slave` class which has most of its functionality. There is not much in the extended class.  The most important part of this extended class is the constructor which chooses the connection method (`SSHLauncher`), and the retention strategy (`SGERetentionStrategy`), it also sets the label which specifies which jobs the agent will be able to execute.
### SGERetentionStrategy.java
This class determines when an idle agent (an agent that isn't doing any job) should be terminated (disconnected). It also takes care of terminating offline agents. So it is a class which checks all agent computer statuses and determines if they should be terminated.
### SGECloud.java
This class checks job labels and determines if an agent should be created. If the label matches the cloud's label the cloud creates a new agent and initiates its connection to the computer through SSH by giving it the credentials which are provided by the user when creating the cloud.

The configuration section interface for this cloud is generated from `SGECloud/config.jelly`.
### SGEBuilder.java
This class represents the build step that can be added in a job's configuration. This class has the biggest part of the whole plugin functionality, the whole process of batch job submission and monitoring and all of the available configurations (from the build step page) involved in it are executed here. The `perform` method is called when a job with the build step `Run job on SGE` is run, this method is the main method and calls every other method of this class and the `BatchSystem` (`SGE`) to perform the interaction between Jenkins and `SGE`.

The configuration section for this build step is generated from `SGEBuilder/config.jelly`. It has all the input fields for all the build step configurations and the batch job itself. This section has another section inside it which is in `SGEBuilder/startUpload.jelly`, it has the interface for file uploading and when a file is uploaded or deleted only this section is updated instead of the whole page.
