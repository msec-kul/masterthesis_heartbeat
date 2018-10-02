package be.kuleuven.msec.iot.iotframework.generic.devicelayer;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;


import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;




/**
 * Created by michielwillocx on 18/09/17.
 * This class represents an IoT Sensor
 */

public abstract class VirtualIoTDevice {

    String systemID;

    public VirtualIoTDevice(String systemID) {
        this.systemID = systemID;
    }

    public String getSystemID() {
        return systemID;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof VirtualIoTDevice) {
            VirtualIoTDevice vid = (VirtualIoTDevice)o;
            return vid.systemID.equals(systemID);
        }
        return false;
    }

    public abstract void isReachable(OnRequestCompleted<Boolean> orc);

    public abstract void monitorReachability(OnEventOccurred<Boolean> oeo);

    public abstract void connect(OnRequestCompleted<Boolean> orc);

    public abstract void disconnect(OnRequestCompleted<Boolean> orc);

}