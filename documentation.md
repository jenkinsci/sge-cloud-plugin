# BatchSystem.java
This is an abstract class, all of its methods are abstract, and it represents all batch systems. It defines the interaction with the batch system methods: submit the job; kill the job; get the status of the job; get the output of a running job (and format it for clear reading); get the output of a finished job; check if a given status is an ending status, running status, job ended with errors or job ended successfully; print the error log and exit code; execute specific actions depending on the status of the job. This class must be extended by specific batch systems like `SGE` and have its methods implemented depending on the specifics of the batch system.
# SGE.java
This class extends the `BatchSystem` class and implements all of its methods. The `BatchSystem` methods are implemented using the actions and commands specific to `SGE` batch system. The interaction with `SGE` is realized through execution of shell commands and extraction of needed information from the output of the commands.
# SGESlave.java
This class represents the slave created by the cloud when a job with the appropriate label is run. It extends the `Slave` class which has most of its functionality. There is not much in the extended class.  The most important part of this extended class is the constructor which chooses the connection to the slave method (`SSHLauncher`), and the retention strategy (`SGERetentionStrategy`), it also sets the label which specifies which jobs the slave will be able to execute.
# SGERetentionStrategy.java
This class determines when an idle slave (a slave who isn't doing any job) should be terminated (disconnected). It also takes care of terminating offline slaves. So it is a class which checks all slave computer status and determines if they should be terminated.
# SGECloud.java
This class checks job labels and determines if a slave should be created. If the label matches the cloud's label the cloud creates a new slave and initiates its connection to the computer through SSH by giving it the credentials which are provided by the user when creating the cloud.

The configuration section interface for this cloud is generated from `SGECloud/config.jelly`.
# SGEBuilder.java
This class represents the build step that can be added in a job's configuration. This class has the biggest part of the whole plugin functionality, the whole process of batch job submission and monitoring and all of the available configurations (from the build step page) involved in it are executed here. The `perform` method is called when a job with the build step `Run job on SGE` is run, this method is the main method and calls every other method of this class and the `BatchSystem` (`SGE`) to perform the interaction between Jenkins and `SGE`.

The configuration section for this build step is generated from `SGEBuilder/config.jelly`. It has all the input fields for all the build step configurations and the batch job itself. This section has another section inside it which is in `SGEBuilder/startUpload.jelly`, it has the interface for file uploading and when a file is uploaded or deleted only this section is updated instead of the whole page.
