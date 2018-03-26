package io.github.sac;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sachin on 16/11/16.
 */

public class ReconnectStrategy {

    private final static Logger LOGGER = Logger.getLogger(ReconnectStrategy.class.getName());
    /**
     * The number of milliseconds to delay before attempting to reconnect.
     * Default: 2000
     */

    int reconnectInterval;

    /**
     * The maximum number of milliseconds to delay a reconnection attempt.
     * Default: 30000
     */

    int maxReconnectInterval;

    /**
     * The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist.
     * Default: 1
     */

    float reconnectDecay;

    /**
     * The maximum number of reconnection attempts that will be made before giving up. If null, reconnection attempts
     * will be continue to be made forever.
     * Default: null
     */

    Integer maxAttempts;

    Integer attemptsMade;


    public ReconnectStrategy() {
        LOGGER.setLevel(Level.INFO);
        reconnectInterval = 2000;
        maxReconnectInterval = 30000;
        reconnectDecay = (float) 1;
        maxAttempts = null;  //forever
        attemptsMade = 0;
    }

    public ReconnectStrategy setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public ReconnectStrategy setDelay(int delay) {
        reconnectInterval = delay;
        return this;
    }

    public void setAttemptsMade(Integer attemptsMade) {
        this.attemptsMade = attemptsMade;
    }

    public ReconnectStrategy(int reconnectInterval, int maxReconnectInterval, float reconnectDecay, int maxAttempts) {
        if (reconnectInterval > maxReconnectInterval) {
            this.reconnectInterval = maxReconnectInterval;
        } else {
            this.reconnectInterval = reconnectInterval;
        }
        this.maxReconnectInterval = maxReconnectInterval;
        this.reconnectDecay = reconnectDecay;
        this.maxAttempts = maxAttempts;
        attemptsMade = 0;
    }


    public void processValues() {
        attemptsMade++;
        LOGGER.info("Attempt number :" + attemptsMade);
        if (reconnectInterval < maxReconnectInterval) {
            reconnectInterval = (int) (reconnectInterval * reconnectDecay);
            if (reconnectInterval > maxReconnectInterval) {
                reconnectInterval = maxReconnectInterval;
            }
        }
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }


    public boolean areAttemptsComplete() {
        return attemptsMade.equals(maxAttempts);
    }

}
