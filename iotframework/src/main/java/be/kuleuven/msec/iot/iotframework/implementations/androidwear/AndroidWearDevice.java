package be.kuleuven.msec.iot.iotframework.implementations.androidwear;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Accelerometer;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Gyroscope;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.HeartRateSensor;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.VirtualIoTConnector;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.VirtualIoTDevice;
import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

/**
 * Created by Thomas on 12/12/2017.
 * This class represents an Android Wear device, which contains one or multiple sensors
 */

public class AndroidWearDevice extends VirtualIoTConnector {
    List<VirtualIoTDevice> sensors = new ArrayList();
    private HeartRateSensor heartRateSensor;
    private Accelerometer accelerometer;
    private Gyroscope gyroscope;
    private AndroidWearClient client;
    private Context context; // https://www.linkedin.com/pulse/android-dev-tips-how-get-static-application-context-kropachov

    //private String[] sensorIds; // Todo map to List<VirtualIoTDevice>

    private String TAG = "AndroidWearDevice";

    public AndroidWearDevice(Context context, String nodeId) {
        super(nodeId);
        this.context = context;
        client = new AndroidWearClient(nodeId, context);
    }

    @Override
    public void updateConnectedDeviceList(OnRequestCompleted<Boolean> orc) {
        // Request available sensors on android device
        client.requestSensors(new OnRequestCompleted<String[]>() {
            @Override
            public void onSuccess(String[] response) {
                // Response is list of sensor ids
                Log.d(TAG, "Sensors: " + response.toString());
                for(String s : response) {
                    if(s.contains("Heart_Rate_Sensor")) {
                        heartRateSensor = new AndroidWearHeartRateSensor(s, client);
                    } else if(s.contains("Accelerometer_Sensor")) {
                        accelerometer = new AndroidWearAccelerometer(s, client);
                    } else if(s.contains("Gyroscope_Sensor")) {
                        gyroscope = new AndroidWearGyroscope(s, client);
                    }
                }
                orc.onSuccess(true);
            }
        });
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
        try {
            client.monitorReachability(oeo);
        } catch(Exception e) {
            oeo.onErrorOccurred(e);
        }
    }

    @Override
    public void connect(OnRequestCompleted<Boolean> orc) {
        client.connect(orc);
    }

    @Override
    public void disconnect(OnRequestCompleted<Boolean> orc) {
        client.disconnect(orc);
    }

    public Accelerometer getAccelerometer() {
        return accelerometer;
    }

    public HeartRateSensor getHeartRateSensor() {
        return heartRateSensor;
    }

    public Gyroscope getGyroscope() { return gyroscope; }

}
