package be.kuleuven.msec.iot.iotframework.generic.devicelayer;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 8/05/2018.
 */

public abstract class Gyroscope extends VirtualIoTDevice {
    private String unit; // in rad/s

    public Gyroscope(String systemID, String unit) {
        super(systemID);
        this.unit = unit;
    }

    public abstract void requestRateOfRotation(OnRequestCompleted<float[]> orc);

    public abstract void monitorRateOfRotation(OnEventOccurred<float[]> oeo);

    public abstract void unmonitorRateOfRotation();

    public String getUnit() {
        return unit;
    }
}
