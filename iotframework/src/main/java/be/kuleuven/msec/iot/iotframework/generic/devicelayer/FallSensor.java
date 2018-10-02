package be.kuleuven.msec.iot.iotframework.generic.devicelayer;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;

/**
 * Created by Thomas on 7/05/2018.
 */

public abstract class FallSensor extends VirtualIoTDevice {
    public FallSensor(String systemID) {super(systemID);}

    public abstract void detectFall(OnEventOccurred<Boolean> orc);
}
