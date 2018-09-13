/*
 * The MIT License
 *
 * Copyright 2015 Laisvydas Skurevicius.
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

import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import static java.util.concurrent.TimeUnit.MINUTES;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Laisvydas Skurevicius
 */
public class BatchRetentionStrategy extends RetentionStrategy<SlaveComputer> {

    // The amount of minutes until a slave is terminated when idle
    private final int idleTerminationMinutes;

    private static final Logger LOGGER = Logger
            .getLogger(BatchRetentionStrategy.class.getName());

    public BatchRetentionStrategy(int idleTerminationMinutes) {
        this.idleTerminationMinutes = idleTerminationMinutes;
    }

    /**
     * This method will be called periodically to allow this strategy to decide
     * whether to terminate the specified {@link hudson.model.Computer}.
     *
     * @param computer {@link hudson.model.Computer} for which this strategy is
     * assigned. This computer may be online or offline.
     * @return The number of minutes after which this strategy would like to be
     * checked again. The strategy may be rechecked earlier or later that this!
     */
    @Override
    public long check(SlaveComputer computer) {
    	final int MINUTES_TO_NEXT_CHECK = 1;
    	
        if (computer.getNode() == null) {
            return MINUTES_TO_NEXT_CHECK;
        }

        if ((System.currentTimeMillis() - computer.getConnectTime())
                < MINUTES.toMillis(idleTerminationMinutes)) {
            return MINUTES_TO_NEXT_CHECK;
        }

        if (computer.isOffline()) {
            LOGGER.log(Level.INFO, "Disconnecting offline computer {0}",
                    computer.getName());
            terminateComputer(computer);
            return MINUTES_TO_NEXT_CHECK;
        }

        if (computer.isIdle()) {
            final long idleMilliseconds
                    = System.currentTimeMillis()
                    - computer.getIdleStartMilliseconds();

            if (idleMilliseconds > MINUTES.toMillis(idleTerminationMinutes)) {
                LOGGER.log(Level.INFO, "Disconnecting idle computer {0}",
                        computer.getName());
                terminateComputer(computer);
            }
        }
        return MINUTES_TO_NEXT_CHECK;
    }

    /** Terminate the slave node if it exists.
     *
     * @param computer Slave computer
     */
    private void terminateComputer(SlaveComputer computer) {
        BatchSlave slave = (BatchSlave) computer.getNode();
        if (slave != null) {
            slave.terminate();
        }
    }

    @Override
    public void start(SlaveComputer computer) {
        computer.connect(false);
    }

    public static class DescriptorImpl
            extends Descriptor<RetentionStrategy<?>> {

        @Override
        public String getDisplayName() {
            return "SGE";
        }
    }

}
