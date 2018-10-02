package be.kuleuven.msec.iot.iotframework.generic.devicelayer;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 12/12/2017.
 */

public abstract class HeartRateSensor extends VirtualIoTDevice {

    private String unit;
    //private int samplingRate;

    public HeartRateSensor(String systemID, String unit) {
        super(systemID);
        this.unit = unit;
    }

    public abstract void requestHeartRate(OnRequestCompleted<Integer> orc);

    public abstract void monitorHeartRate(OnEventOccurred<Integer> oeo);

    public abstract void unmonitorHeartRate();

    // Trigger event when heart rate exceeds given value
    public abstract void exceeds(int value, OnEventOccurred<Integer> oeo);

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
}
