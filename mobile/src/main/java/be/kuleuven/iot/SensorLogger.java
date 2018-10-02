package be.kuleuven.iot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Thomas on 12/05/2018.
 */

public class SensorLogger {
    private HashMap<String, List<SensorLogEntry>> log = new HashMap<>();

    public void log(String sensorID, float[] values) {
        List entries = log.get(sensorID);
        if(entries == null) {
            ArrayList<SensorLogEntry> entr = new ArrayList<>();
            entr.add(new SensorLogEntry(System.currentTimeMillis(), values));
            log.put(sensorID, entr);
        } else {
            entries.add(new SensorLogEntry(System.currentTimeMillis(), values));
        }
    }

    public void clear() {
        log = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(String sensor : log.keySet()) {
            sb.append(sensor + "\n");
            for(SensorLogEntry entry : log.get(sensor)) {
                sb.append(entry.toString() + "\n");
            }
        }
        return sb.toString();
    }
}

class SensorLogEntry {
    public long time;
    public float[] values;

    public SensorLogEntry(long time, float[] values) {
        this.time = time;
        this.values = values;
    }

    @Override
    public String toString() {
        return time + " " + Arrays.toString(values);
    }
}
