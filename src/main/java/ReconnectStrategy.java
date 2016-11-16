import com.neovisionaries.ws.client.WebSocketException;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sachin on 16/11/16.
 */

public class ReconnectStrategy {

    /**
     *The number of milliseconds to delay before attempting to reconnect.
     * Default: 1000
     */

    int reconnectInterval;

    /**
     * The maximum number of milliseconds to delay a reconnection attempt.
     * Default: 30000
     */

    int maxReconnectInterval;

    /**
     *The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist.
     * Default: 1.5
     *
     */

    float reconnectDecay;

    /**
     * The maximum number of reconnection attempts that will be made before giving up. If null, reconnection attempts will be continue to be made forever.
     * Default: null
     */

    Integer maxAttempts;

    Integer attmptsMade;


    ReconnectStrategy(){
        reconnectInterval=1000;
        maxReconnectInterval=30000;
        reconnectDecay= (float) 1.5;
        maxAttempts=null;  //forever
        attmptsMade=0;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public ReconnectStrategy(int reconnectInterval, int maxReconnectInterval, float reconnectDecay, int maxAttempts) {
        if (reconnectInterval>maxReconnectInterval) {
            this.reconnectInterval = maxReconnectInterval;
        }else {
            this.reconnectInterval=reconnectInterval;
        }
        this.maxReconnectInterval = maxReconnectInterval;
        this.reconnectDecay = reconnectDecay;
        this.maxAttempts = maxAttempts;
        attmptsMade=0;
    }


    public interface Callback{
        void connect();
    }

    public void setListener(final Callback callback){

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                attmptsMade++;
                    callback.connect();
                System.out.println("value of reconnect interval is"+reconnectInterval);
                if (reconnectInterval<maxReconnectInterval) {
                    reconnectInterval = (int) (reconnectInterval * reconnectDecay);
                    if (reconnectInterval>maxReconnectInterval){
                        reconnectInterval=maxReconnectInterval;
                    }
                }
            }
        },reconnectInterval);
    }

    public boolean areAttemptsComplete(){
        return attmptsMade.equals(maxAttempts);
    }

}
