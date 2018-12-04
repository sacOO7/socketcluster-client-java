package io.github.sac;

/**
 * Created by sachin on 15/6/17.
 */

public class ReconnectionStrategy {
    private int MaxAttempts;
    private int numberOfAttempts;
    private int reconnectInterval;
    private int maxReconnectInterval = 30000;

    public ReconnectionStrategy(int maxAttempts, int reconnectInterval) {
        MaxAttempts = maxAttempts;
        if (reconnectInterval < maxReconnectInterval) {
            this.reconnectInterval = reconnectInterval;
        } else {
            this.reconnectInterval = maxReconnectInterval;
        }
        numberOfAttempts = 0;
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    public void setNumberOfAttempts(int numberOfAttempts) {
        this.numberOfAttempts = numberOfAttempts;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void processAttempts() {
        numberOfAttempts++;
    }

    public int getMaxAttempts() {
        return MaxAttempts;
    }
}
