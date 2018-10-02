package be.kuleuven.msec.iot.iotframework.implementations.androidwear;

import android.util.Log;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Gyroscope;
import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

/**
 * Created by Thomas on 8/05/2018.
 */

public class AndroidWearGyroscope extends Gyroscope {
    private AndroidWearClient gateway;
    private final static String TAG = "AndroidWearGyroscope";

    public AndroidWearGyroscope(String identifier, AndroidWearClient gateway) {
        super(identifier, "rad/s");
        this.gateway = gateway;
    }

    @Override
    public void requestRateOfRotation(OnRequestCompleted<float[]> orc) {
        gateway.requestSensorValue(new OnRequestCompleted<float[]>() {
            @Override
            public void onSuccess(float[] response) {
                if(response.length > 0) {
                    orc.onSuccess(response);
                } else {
                    Log.d(TAG, "No data received");
                }
            }
        }, this.getSystemID());
    }

    @Override
    public void monitorRateOfRotation(OnEventOccurred<float[]> oeo) {
        gateway.monitorSensor(oeo, this.getSystemID());
    }

    @Override
    public void unmonitorRateOfRotation() {
        gateway.unmonitor(this.getSystemID());
    }

    @Override
    public void isReachable(OnRequestCompleted<Boolean> orc) {
        gateway.isReachable(orc);
    }

    @Override
    public void monitorReachability(OnEventOccurred<Boolean> oeo) {
        gateway.monitorReachability(oeo);
    }

    @Override
    public void connect(OnRequestCompleted<Boolean> orc) {

    }

    @Override
    public void disconnect(OnRequestCompleted<Boolean> orc) {

    }
}
