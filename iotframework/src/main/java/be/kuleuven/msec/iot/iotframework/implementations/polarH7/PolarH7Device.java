package be.kuleuven.msec.iot.iotframework.implementations.polarH7;

import android.content.Context;

import java.util.LinkedList;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.HeartRateSensor;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.VirtualIoTConnector;
import be.kuleuven.msec.iot.iotframework.implementations.polarH7.communication.PolarH7Client;

/**
 * Created by Thomas on 6/03/2018.
 */

public class PolarH7Device extends VirtualIoTConnector {
    private PolarH7Client client;
    PolarH7HeartRateSensor heartRateSensor;

    private LinkedList<OnRequestCompleted<Boolean>> reachabilityRequests = new LinkedList<>();

    public PolarH7Device(Context context, String systemID) {
        super(systemID);
        client = new PolarH7Client(context, systemID);
        heartRateSensor = new PolarH7HeartRateSensor(client);
    }


    public void register() {

    }

    public void unregister() {
        //bluetoothAdapter.;
    }

    @Override
    public void updateConnectedDeviceList(OnRequestCompleted<Boolean> orc) {
        orc.onSuccess(true);
    }

    @Override
    public void initialize(OnRequestCompleted orc) {

    }

    @Override
    public void isReachable(OnRequestCompleted<Boolean> orc) {
        client.isReachable(orc);
    }

    @Override
    public void monitorReachability(OnEventOccurred<Boolean> oeo) {

    }

    @Override
    public void connect(OnRequestCompleted<Boolean> orc) {
        client.connect(orc);
    }

    @Override
    public void disconnect(OnRequestCompleted<Boolean> orc) {
        client.disconnect();
    }

    public HeartRateSensor getHeartRateSensor() {
        return this.heartRateSensor;
    }
}
