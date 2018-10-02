package be.kuleuven.msec.iot.iotframework.callbackinterfaces;

/**
 * Created by michielwillocx on 12/09/17.
 */

public interface OnEventOccurred<T> {
    public void onUpdate(T response);

    default void onErrorOccurred(Exception exception) {

        new UnsupportedOperationException("OnErrorOccured not implemented", exception).printStackTrace();

    }
}