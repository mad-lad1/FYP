package model.repositories.rosRepo.connection;

import android.os.AsyncTask;



import model.entities.MasterEntity;
import utility.Utils;

/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.1
 * @created on 15.04.20
 * @updated on 16.04.20
 * @modified by
 */
public class ConnectionCheckTask extends AsyncTask<MasterEntity, Void, Boolean> {

    private static final int TIMEOUT_TIME = 2 * 1000;

    private final ConnectionListener listener;

    public ConnectionCheckTask(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(MasterEntity... masterEnts) {
        MasterEntity masterEnt = masterEnts[0];
        return Utils.isHostAvailable("192.168.43.118", 11311, TIMEOUT_TIME);
    }



    @Override
    protected void onPostExecute(Boolean success) {
        if (success)
            listener.onSuccess();
        else
            listener.onFailed();
    }
}
