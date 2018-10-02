package be.kuleuven.msec.iot.iotframework.implementations.androidwear;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Accelerometer;
import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

/**
 * Created by Thomas on 6/03/2018.
 */

public class AndroidWearAccelerometer extends Accelerometer {
    private AndroidWearClient gateway;

    public AndroidWearAccelerometer(String systemID, AndroidWearClient gateway) {
        super(systemID, "m/s^2");
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
    public void requestAcceleration(OnRequestCompleted<float[]> orc) {
        gateway.requestSensorValue(orc, this.getSystemID());
    }

    @Override
    public void monitorAcceleration(OnEventOccurred<float[]> oeo) {
        gateway.monitorSensor(oeo, this.getSystemID());
    }

    @Override
    public void unmonitorAcceleration() {
        gateway.unmonitor(this.getSystemID());
    }

/*    @Override
    public void changeSamplingRate(int samplingRate, OnRequestCompleted<Boolean> orc) {

    }*/

    @Override
    public void detectFall(OnEventOccurred<float[]> ore) {
        gateway.fallDetected(ore, this.getSystemID());
    }
}
