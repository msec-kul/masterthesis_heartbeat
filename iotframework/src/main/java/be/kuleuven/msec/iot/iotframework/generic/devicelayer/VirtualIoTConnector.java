package be.kuleuven.msec.iot.iotframework.generic.devicelayer;


import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by michielwillocx on 20/09/17.
 * This class represents an IoT gateway, which allows access to one or multiple IoT sensors
 */

public abstract class VirtualIoTConnector {

    String systemID;

    public VirtualIoTConnector(String systemID) {
        this.systemID = systemID;
    }

    public String getSystemID() {
        return systemID;
    }

    public abstract void updateConnectedDeviceList(OnRequestCompleted<Boolean> orc);

    public abstract void initialize(OnRequestCompleted orc);

    public abstract void isReachable(OnRequestCompleted<Boolean> orc);

    public abstract void monitorReachability(OnEventOccurred<Boolean> oeo);

    public abstract void connect(OnRequestCompleted<Boolean> orc);

    public abstract void disconnect(OnRequestCompleted<Boolean> orc);
}
