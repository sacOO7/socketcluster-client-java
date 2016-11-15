/**
 * Created by sachin on 16/11/16.
 */

/**
 *  Interface for handling errors
 *
 */

public interface Ack {
    public void call(Object error,Object data);
}
