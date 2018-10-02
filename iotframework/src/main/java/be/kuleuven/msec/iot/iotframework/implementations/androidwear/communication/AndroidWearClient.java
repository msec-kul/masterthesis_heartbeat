package be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 12/12/2017.
 * This class is used to communicate with a certain Android Wear device
 */

public class AndroidWearClient implements DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener {

    public final static String REQUEST_SENSORS = "RequestSensors";
    public final static String RESPOND_SENSORS = "RespondSensors";
    public final static String REQUEST_SENSOR_VALUE = "RequestSensorValue";
    public final static String RESPOND_SENSOR_VALUE = "RespondSensorValue";
    public final static String REQUEST_SENSOR_MONITORING = "RequestSensorMonitoring";
    public final static String REQUEST_SENSOR_VALUE_EXCEEDED = "RequestSensorValueExceeded";
    //public final static String RESPOND_SENSOR_MONITORING = "RespondSensorMonitoring";
    public final static String CHANGE_SAMPLING_RATE = "ChangeSamplingRate";
    public final static String UNMONITOR_SENSOR = "UnmonitorSensor";
    public final static String RESPOND_VALUE_EXCEEDED = "RespondValueExceeded";
    public final static String DISCONNECT = "Disconnect";

    public final static String SENSORVALUE_PATH = "value";
    public final static String SENSORTYPE_PATH = "type";
    public final static String SENSORNAME_PATH = "name";

    private final static String TAG = "AndroidWearClient";

    private String nodeId;
    private Context context;

    private boolean isReachable = false;

    private LinkedList<OnRequestCompleted> requestQueue = new LinkedList<>();
    private HashMap<String, OnEventOccurred> sensorMonitorMap = new HashMap<>();
    private ArrayList<OnEventOccurred<Boolean>> reachabilityMonitors = new ArrayList<>();
    private HashMap<String, OnEventOccurred<float[]>> onFallMonitors = new HashMap<>();
    private HashMap<String, float[]> lastSensorValues = new HashMap<>();
    private HashMap<String, OnEventOccurred> exceedsMap = new HashMap<>();

    public AndroidWearClient(String nodeId, Context context) {
        this.nodeId = nodeId;
        this.context = context;
/*        Wearable.getMessageClient(context.getApplicationContext()).addListener(this);
        Wearable.getDataClient(context.getApplicationContext()).addListener(this);*/
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        // Used for sending one-way messages
        switch(messageEvent.getPath()) {
            case RESPOND_SENSORS:
                OnRequestCompleted orc = requestQueue.pop();
                if(orc != null) {
                    String data = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                    orc.onSuccess(data.split("-"));
                }
                break;
            case RESPOND_VALUE_EXCEEDED:
                String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                String sensorId = message.substring(0, message.indexOf("|"));
                String valuesString = message.substring(message.indexOf("|") + 1);
                Log.d(TAG, sensorId);
                Log.d(TAG, valuesString);
                float[] values = stringToArray(valuesString);
                OnEventOccurred oeo = exceedsMap.get(sensorId);
                // Todo: cast to correct type, based on generic in callback
                if(oeo != null & values.length > 0) oeo.onUpdate((int)values[0]);
                break;
        }
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    public static int byteArrayToInt(byte[] value) {
        int result = 0;
        for(int i = 0; i < value.length; i++) {
            result += value[i];
            if(i != value.length - 1)
                result = result << 8;
        }
        return result;
    }

    public void monitorSensor(OnEventOccurred oeo, String sensorId) {
        Task<Integer> task = Wearable.getMessageClient(context).sendMessage(nodeId, REQUEST_SENSOR_MONITORING, sensorId.getBytes(StandardCharsets.UTF_8));
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                Log.d(TAG, "Sensor monitor message delivered!");
                sensorMonitorMap.put(sensorId, oeo);
            }
        });
    }

    public void unmonitor(String sensorId) {
        Task<Integer> task = Wearable.getMessageClient(context).sendMessage(nodeId, UNMONITOR_SENSOR, sensorId.getBytes(StandardCharsets.UTF_8));
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                Log.d(TAG, "Sensor unmonitor message delivered!");
                sensorMonitorMap.remove(sensorId);
                onFallMonitors.remove(sensorId);
                exceedsMap.remove(sensorId);
            }
        });
    }

    public void sensorValueExceeded(float[] values, OnEventOccurred oeo, String sensorId) {
        exceedsMap.put(sensorId, oeo);
        Wearable.getMessageClient(context).sendMessage(nodeId, REQUEST_SENSOR_VALUE_EXCEEDED, (sensorId + "|" + Arrays.toString(values)).getBytes(StandardCharsets.UTF_8));
    }

    public void monitorReachability(OnEventOccurred<Boolean> oeo) {
        reachabilityMonitors.add(oeo);
    }

    public void requestSensorValue(OnRequestCompleted<float[]> orc, String sensorId) {
        Task<Integer> task = Wearable.getMessageClient(context).sendMessage(nodeId, REQUEST_SENSOR_VALUE, sensorId.getBytes(StandardCharsets.UTF_8));
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                requestQueue.add(orc);
            }
        });
    }

    public void fallDetected(OnEventOccurred<float[]> orc, String sensorId) {
        // Check for fall on mobile device
        // TODO move this to gateway
        onFallMonitors.put(sensorId, orc);
    }

    public void requestSensors(OnRequestCompleted<String[]> request) {
        requestQueue.add(request);
        new RequestSensorsTask().execute();
    }

    public void disconnect(OnRequestCompleted<Boolean> orc) {
        // Stop monitoring of sensors
        Task<Integer> task = Wearable.getMessageClient(context).sendMessage(nodeId, DISCONNECT, null);
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                Wearable.getMessageClient(context.getApplicationContext()).removeListener(AndroidWearClient.this);
                Wearable.getDataClient(context.getApplicationContext()).removeListener(AndroidWearClient.this);
                orc.onSuccess(true);
            }
        });
    }

    public void connect(OnRequestCompleted<Boolean> orc) {
        Wearable.getMessageClient(context.getApplicationContext()).addListener(this);
        Wearable.getDataClient(context.getApplicationContext()).addListener(this);
        // Resume monitoring of sensors
        for(String sensorId : sensorMonitorMap.keySet()) {
            Wearable.getMessageClient(context).sendMessage(nodeId, REQUEST_SENSOR_MONITORING, sensorId.getBytes(StandardCharsets.UTF_8));
        }
        for(String sensorId : exceedsMap.keySet()) {
            Wearable.getMessageClient(context).sendMessage(nodeId, REQUEST_SENSOR_MONITORING, sensorId.getBytes(StandardCharsets.UTF_8));
        }
        orc.onSuccess(true);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        // Used for synchronising data using data maps
        Log.d(TAG, "Data changed " + dataEventBuffer);
        for(DataEvent dataEvent : dataEventBuffer) {
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                // Check if sensorMap contains URI (=sensorId)
                String[] uri = dataEvent.getDataItem().getUri().toString().split("/");
                if(uri.length > 0) {
                    String sensorId = uri[uri.length - 1];
                    // Update monitor
                    OnEventOccurred oeo = sensorMonitorMap.get(sensorId);
                    if(oeo != null) {
                        // Sensormap has event
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                        float[] sensorValues = dataMapItem.getDataMap().getFloatArray(SENSORVALUE_PATH);

                        oeo.onUpdate(sensorValues);
                    }

                    // Check if fall detected
                    OnEventOccurred o = onFallMonitors.get(sensorId);
                    if(o != null) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                        float[] sensorValues = dataMapItem.getDataMap().getFloatArray(SENSORVALUE_PATH);
                        float[] lastValues;
                        if(sensorValues.length > 2 && (lastValues = lastSensorValues.get(sensorId)) != null && lastValues.length > 2) {
                            // Calculate delta
                            double gs = Math.sqrt(Math.pow(sensorValues[0] - lastValues[0], 2) + Math.pow(sensorValues[1] - lastValues[1], 2) + Math.pow(sensorValues[2] - lastValues[2], 2));
                            Log.d(TAG, "delta: " + gs);
                            // Check if delta exceeds 3G's
                            if(gs > (3d * 9.81)) {
                                // Fall detected
                                o.onUpdate(sensorValues);
                            }
                        }
                    }

                    if(oeo != null) {
                        // Update previous sensorValues
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                        float[] sensorValues = dataMapItem.getDataMap().getFloatArray(SENSORVALUE_PATH);
                        lastSensorValues.put(sensorId, sensorValues);
                    }
                }
            }
        }
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Set<Node> nodes = capabilityInfo.getNodes();
        if(nodes.isEmpty()) {
            isReachable = false;
        } else {
            for(Node n : capabilityInfo.getNodes()) {
                if(n.getId().equals(this.nodeId)) {
                    isReachable = true;
                    break;
                }
                isReachable = false;
            }
        }
        for(OnEventOccurred<Boolean> oeo : reachabilityMonitors) {
            oeo.onUpdate(isReachable);
        }
    }

    private class RequestSensorsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Task<Integer> task = Wearable.getMessageClient(context.getApplicationContext()).sendMessage(nodeId, REQUEST_SENSORS, new byte[0]);
            try {
                Tasks.await(task);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void isReachable(OnRequestCompleted<Boolean> orc) {
        Wearable.getNodeClient(context).getConnectedNodes().addOnSuccessListener(new OnSuccessListener<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                isReachable = false;
                for(Node node : nodes) {
                    if(node.getId().equals(nodeId)) {
                        isReachable = true;
                        break;
                    }
                }
                orc.onSuccess(isReachable);
            }
        });
    }

    public static float[] stringToArray(String array) {
        // array looks like [1.02,12.54,538]
        if(array.length() > 0) {
            String values = array.substring(1, array.length() - 1);
            String[] stringArray = values.split(",");
            float[] floatValues = new float[stringArray.length];
            for(int i = 0; i < stringArray.length; i++) {
                floatValues[i] = Float.parseFloat(stringArray[i]);
            }
            return floatValues;
        }
        return new float[0];
    }
}