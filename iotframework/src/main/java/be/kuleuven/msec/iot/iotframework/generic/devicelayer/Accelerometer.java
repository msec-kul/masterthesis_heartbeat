package be.kuleuven.msec.iot.iotframework.generic.devicelayer;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 6/03/2018.
 */

public abstract class Accelerometer extends VirtualIoTDevice {

    //private float xVal, yVal, zVal;
    private String unit;
    //private int samplingRate;

    public Accelerometer(String systemID, String unit) {
        super(systemID);
        this.unit = unit;
    }

    public abstract void requestAcceleration(OnRequestCompleted<float[]> orc);

    public abstract void monitorAcceleration(OnEventOccurred<float[]> oeo);

    public abstract void unmonitorAcceleration();

    //public abstract void changeSamplingRate(int samplingRate, OnRequestCompleted<Boolean> orc);

    public String getUnit() {
        return unit;
    }

/*    public void setUnit(String unit) {
        this.unit = unit;
    }*/

/*    public int getSamplingRate() {
        return samplingRate;
    }*/

    // Triggers event if fall is detected and supplies the event with the acceleration in each direction
    public abstract void detectFall(OnEventOccurred<float[]> ore);
}
