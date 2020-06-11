# Job Exit Status

When a program finishes executing it returns an exit code to the system.
The exit code (also called "exit status") is an integer from 0 to 255.
The batch system reports this exit code. There are two general ways for
the exit code of a program to be set.

## The Program Exits

The program can explicitly call `exit(exit_code)` (or return
from `main()`, which eventually calls `exit()`). In this case the exit
code is the argument to `exit(exit_code)` and its meaning depends on the
program. By convention, these exit codes are limited to the range 0 to
127.

The program executes the last instruction in `main()` without
calling `exit()` or `return`. In this case the system sets the exit code
to 0.

## A Signal Is Received

The other way that a program can terminate is due to the receipt of a signal.
In the absence of a signal handler, the system sets the exit code to
*signalNumber* + 128. If the program has a signal handler that calls
`exit(exit_code)`, then the specified exit code is returned.

### Signal Numbers

The following table lists the various signals whose default action is to
terminate a program. Note that the codes can vary depending on your
platform. The numbers in the table below show the signal numbers for Red
Hat Enterprise Linux on Intel x86\_64. The ultimate authority is
the `signal.h` header file in your system. Command `kill -l` will also print
a compact summary of signal numbers and names.

| No. | Name      | Description                                                                                                                                                                                                                                                                                                                                                                          |
|-----|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | SIGHUP    | If a process is being run from terminal and that terminal suddenly goes away then the process receives this signal. “HUP” is short for “hang up \[the telephone\]”. See the man page for `nohup(1)` for more information.                                                                                                                                                            |
| 2   | SIGINT    | The process was “interrupted”. This happens when you press Control+C on the controlling terminal.                                                                                                                                                                                                                                                                                    |
| 3   | SIGQUIT   |                                                                                                                                                                                                                                                                                                                                                                                      |
| 4   | SIGILL    | Illegal instruction. The program contained some machine code the CPU can't understand.                                                                                                                                                                                                                                                                                               |
| 5   | SIGTRAP   | This signal is used mainly from within debuggers and program tracers.                                                                                                                                                                                                                                                                                                                |
| 6   | SIGABRT   | The program called the abort() function. This is an emergency stop.                                                                                                                                                                                                                                                                                                                  |
| 7   | SIGBUS    | An attempt was made to access memory incorrectly. This can be caused by alignment errors in memory access etc.                                                                                                                                                                                                                                                                       |
| 8   | SIGFPE    | A floating point exception happened in the program.                                                                                                                                                                                                                                                                                                                                  |
| 9   | SIGKILL   | The process was explicitly killed by somebody wielding the kill program.                                                                                                                                                                                                                                                                                                             |
| 10  | SIGUSR1   | Left for the programmers to do whatever they want.                                                                                                                                                                                                                                                                                                                                   |
| 11  | SIGSEGV   | An attempt was made to access memory not allocated to the process. This is often caused by reading off the end of arrays etc.                                                                                                                                                                                                                                                        |
| 12  | SIGUSR2   | Left for the programmers to do whatever they want.                                                                                                                                                                                                                                                                                                                                   |
| 13  | SIGPIPE   | If a process is producing output that is being fed into another process that consume it via a pipe (“producer \| consumer”) and the consumer dies then the producer is sent this signal.                                                                                                                                                                                             |
| 14  | SIGALRM   | A process can request a “wake up call” from the operating system at some time in the future by calling the alarm() function. When that time comes round the wake up call consists of this signal.                                                                                                                                                                                    |
| 15  | SIGTERM   | The process was explicitly killed by somebody wielding the kill program.                                                                                                                                                                                                                                                                                                             |
| 16  | SIGSTKFLT |                                                                                                                                                                                                                                                                                                                                                                                      |
| 17  | SIGCHLD   | The process had previously created one or more child processes with the fork() function. One or more of these processes has since died.                                                                                                                                                                                                                                              |
| 18  | SIGCONT   | (To be read in conjunction with SIGSTOP.) If a process has been paused by sending it SIGSTOP then sending SIGCONT to the process wakes it up again (“continues” it).                                                                                                                                                                                                                 |
| 19  | SIGSTOP   | (To be read in conjunction with SIGCONT.) If a process is sent SIGSTOP it is paused by the operating system. All its state is preserved ready for it to be restarted (by SIGCONT) but it doesn't get any more CPU cycles until then.                                                                                                                                                 |
| 20  | SIGTSTP   | Essentially the same as SIGSTOP. This is the signal sent when the user hits Control+Z on the terminal. (SIGTSTP is short for “terminal stop”) The only difference between SIGTSTP and SIGSTOP is that pausing is only the default action for SIGTSTP but is the required action for SIGSTOP. The process can opt to handle SIGTSTP differently but gets no choice regarding SIGSTOP. |
| 21  | SIGTTIN   | The operating system sends this signal to a backgrounded process when it tries to read input from its terminal. The typical response is to pause (as per SIGSTOP and SIFTSTP) and wait for the SIGCONT that arrives when the process is brought back to the foreground.                                                                                                              |
| 22  | SIGTTOU   | The operating system sends this signal to a backgrounded process when it tries to write output to its terminal. The typical response is as per SIGTTIN.                                                                                                                                                                                                                              |
| 23  | SIGURG    | The operating system sends this signal to a process using a network connection when “urgent” out of band data is sent to it.                                                                                                                                                                                                                                                         |
| 24  | SIGXCPU   | The operating system sends this signal to a process that has exceeded its CPU limit. You can cancel any CPU limit with the shell command “ulimit -t unlimited” prior to running make though it is more likely that something has gone wrong if you reach the CPU limit in make.                                                                                                      |
| 25  | SIGXFSZ   | The operating system sends this signal to a process that has tried to create a file above the file size limit. You can cancel any file size limit with the shell command “ulimit -f unlimited” prior to running make though it is more likely that something has gone wrong if you reach the file size limit in make.                                                                |
| 26  | SIGVTALRM | This is very similar to SIGALRM, but while SIGALRM is sent after a certain amount of real time has passed, SIGVTALRM is sent after a certain amount of time has been spent running the process.                                                                                                                                                                                      |
| 27  | SIGPROF   | This is also very similar to SIGALRM and SIGVTALRM, but while SIGALRM is sent after a certain amount of real time has passed, SIGPROF is sent after a certain amount of time has been spent running the process and running system code on behalf of the process.                                                                                                                    |
| 28  | SIGWINCH  | (Mostly unused these days.) A process used to be sent this signal when one of its windows was resized.                                                                                                                                                                                                                                                                               |
| 29  | SIGIO     | (Also known as SIGPOLL.) A process can arrange to have this signal sent to it when there is some input ready for it to process or an output channel has become ready for writing.                                                                                                                                                                                                    |
| 30  | SIGPWR    | A signal sent to processes by a power management service to indicate that power has switched to a short term emergency power supply. The process (especially long-running daemons) may care to shut down clean before the emergency power fails.                                                                                                                                     |
| 31  | SIGSYS    | Unused.                                                                                                                                                                                                                                                                                                                                                                              |

### Reasons a Signal is Received

There are several reasons that your program might receive a signal.

#### You Kill the Job

You sent it a signal with the UNIX `kill` command, or SGE
command `qdel`. If you don't specify which signal to
send, `kill` defaults to SIGTERM (exit code 15+128=143) and `qdel` sends
SIGINT (exit code 2+128=130), then SIGTERM, then SIGKILL until your job
dies.

#### The System Sent a Signal

The system on which your job was running sent your job a signal because
an error occurred or a system resource limit was reached. In this case,
in addition to the exit code *signalNumber* + 128, the batch system will
usually report an error message. Examples of this case are:

| Signal  | Exit Code | Typical Reason                                                                                                                                       |
|---------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| SIGILL  | 132       | illegal instruction, binary probably corrupt                                                                                                         |
| SIGTRAP | 133       | integer divide-by-zero                                                                                                                               |
| SIGFPE  | 136       | floating point exception or integer overflow (these exceptions aren't generated unless special action is taken, see man sigfpe for more information) |
| SIGKILL | 137       | The process was explicitly killed by somebody wielding the kill program. Exit status 137 has also been observed when memory size limit was exceeded. |
| SIGBUS  | 138       | unaligned memory access (e.g. loading a word that is not aligned on a word boundary)                                                                 |
| SIGSEGV | 139       | attempt to access a virtual address which is not in your address space                                                                               |
| SIGXCPU | 152       | CPU time limit exceeded or memory size limit exceeded                                                                                                |
| SIGXFSZ | 153       | File size limit exceeded                                                                                                                             |

#### SGE Sent a Signal

The batch system sent it a signal because it exceeded a limit on the
queue it was running in. Three queue limits are enforced in this way:

1.  CPU time or memory usage limit. When the sum of CPU time or memory
    usage of all processes in a batch job exceeds the limits specified
    for the SGE queue, the batch system kills the job by sending SIGXCPU
    (exit code 24+128=152), then SIGINT, then SIGTERM, then SIGKILL
    until the job dies. In this case the exit message says "Exited with
    signal termination: Cputime limit exceeded, and core dumped." Under
    rare circumstances, the system (as opposed to the batch system)
    could kill a job by sending SIGXCPU. In this case the exit message
    would say "Exited with exit code 152."
2.  Wallclock Time limit. When this limit is exceeded, the batch system
    kills the job by sending SIGUSR2, then SIGINT, then SIGTERM, then
    SIGKILL until the job dies.
3.  A third queue limit, the STACKSIZE limit, is enforced by the system
    (rather than the batch system) killing the job by sending a SIGSEGV.
    The exit message says "Exited with exit code 139."

## Acknowledgements

This page is largely based on a [2007 explanation of SGE exit status by
dugan of Boston
University](http://www.bu.edu/tech/files/text/batchcode.txt).

The table of signal numbers and descriptions is based on [UNIX course
notes from the University of
Cambridge](http://www.ucs.cam.ac.uk/docs/course-notes/unix-courses/Building/files/signals.pdf).
