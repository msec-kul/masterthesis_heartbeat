package be.kuleuven.msec.iot.iotframework.implementations.androidwear;

import android.util.Log;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.HeartRateSensor;
import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

/**
 * Created by Thomas on 12/12/2017.
 */

public class AndroidWearHeartRateSensor extends HeartRateSensor {
    private AndroidWearClient gateway;
    private final static String TAG = "AndroidWearHRSensor";

    public AndroidWearHeartRateSensor(String identifier, AndroidWearClient gateway) {
        super(identifier, "BPM");
        this.gateway = gateway;
    }

    @Override
    public void isReachable(OnRequestCompleted<Boolean> orc) {
        gateway.isReachable(orc);
    }

    @Override
    public void monitorReachability(OnEventOccurred<Boolean> oeo) {
        try {
            gateway.monitorReachability(oeo);
        } catch(Exception e) {
            oeo.onErrorOccurred(e);
        }
    }

    @Override
    public void connect(OnRequestCompleted<Boolean> orc) {

    }

    @Override
    public void disconnect(OnRequestCompleted<Boolean> orc) {

    }

    @Override
    public void requestHeartRate(OnRequestCompleted<Integer> orc) {
        // Request heart rate once
        gateway.requestSensorValue(new OnRequestCompleted<float[]>() {
            @Override
            public void onSuccess(float[] response) {
                if(response.length > 0) {
                    orc.onSuccess(Math.round(response[0]));
                } else {
                    Log.d(TAG, "No data received");
                    //orc.onSuccess(0);
                }

            }
        }, this.getSystemID());
    }

    @Override
    public void monitorHeartRate(OnEventOccurred<Integer> oeo) {
        // Send request to wearable to continuously send updates of the heart rate
        gateway.monitorSensor(new OnEventOccurred<float[]>() {
            @Override
            public void onUpdate(float[] response) {
                if (response.length > 0) {
                    oeo.onUpdate(Math.round(response[0]));
                } else {
                    Log.d(TAG, "No data received");
                    //oeo.onUpdate(0);
                }
            }
        }, this.getSystemID());
    }

    @Override
    public void unmonitorHeartRate() {
        gateway.unmonitor(this.getSystemID());
    }

    @Override
    public void exceeds(int value, OnEventOccurred<Integer> oeo) {
        gateway.sensorValueExceeded(new float[] { value }, oeo, this.getSystemID());
    }

/*    @Override
    public void changeSamplingRate(int samplingRate, OnRequestCompleted<Boolean> orc) {
        //gateway.getMessenger().changeSamplingRate()
    }*/
}
