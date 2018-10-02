package be.kuleuven.msec.iot.iotframework.implementations.polarH7;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.HeartRateSensor;
import be.kuleuven.msec.iot.iotframework.implementations.polarH7.communication.PolarH7Client;

/**
 * Created by Thomas on 6/03/2018.
 */

public class PolarH7HeartRateSensor extends HeartRateSensor {
    PolarH7Client gateway;

    public PolarH7HeartRateSensor(PolarH7Client gateway) {
        super("123", "BPM");
        this.gateway = gateway;
    }

    @Override
    public void requestHeartRate(OnRequestCompleted<Integer> orc) {

    }

    @Override
    public void monitorHeartRate(OnEventOccurred<Integer> oeo) {
        gateway.monitorSensor(oeo);
    }

    @Override
    public void unmonitorHeartRate() {
        gateway.unmonitor();
    }

    @Override
    public void exceeds(int value, OnEventOccurred<Integer> oeo) {
        gateway.exceeds(value, oeo);
    }

/*    @Override
    public void changeSamplingRate(int samplingRate, OnRequestCompleted<Boolean> orc) {

    }*/

    @Override
    public void isReachable(OnRequestCompleted<Boolean> orc) {
        gateway.isReachable(orc);
    }

    @Override
    public void monitorReachability(OnEventOccurred<Boolean> oeo) {

    }

    @Override
    public void connect(OnRequestCompleted<Boolean> orc) {
        gateway.connect(orc);
    }

    @Override
    public void disconnect(OnRequestCompleted<Boolean> orc) {
        gateway.disconnect();
    }
}
