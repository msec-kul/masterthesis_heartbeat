package be.kuleuven.msec.iot.iotframework.callbackinterfaces;

/**
 * Created by michielwillocx on 11/09/17.
 */

public interface OnRequestCompleted<T> {

    void onSuccess(T response);

    default void onFailure(Exception exception){
        new UnsupportedOperationException("OnFailure not implemented", exception).printStackTrace();
    }
}
