package be.kuleuven.msec.iot.iotframework.implementations.androidwear;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 26/02/2018.
 */

public class AndroidWearDiscoverer {
    private LinkedList<OnRequestCompleted<List<String>>> orcQueue = new LinkedList<>();
    private Context context;

    public AndroidWearDiscoverer(Context context) {
        this.context = context;
    }

    public void getConnectedNodeIds(OnRequestCompleted<List<String>> orc) {
            orcQueue.push(orc);
        new RetrieveConnectedNodesTask().execute();
    }

    private class RetrieveConnectedNodesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            Task<List<Node>> task = Wearable.getNodeClient(context.getApplicationContext()).getConnectedNodes();
            ArrayList<String> nodeIds = new ArrayList<>();
            try {
                List<Node> nodes = Tasks.await(task);
                for(Node n : nodes) {
                    nodeIds.add(n.getId());
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return nodeIds;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            OnRequestCompleted<List<String>> orc = orcQueue.pop();
            if(orc != null)
                orc.onSuccess(strings);
        }
    }
}
