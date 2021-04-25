package model.repositories.rosRepo;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import java.util.ArrayList;

import model.entities.MasterEntity;
import model.repositories.domain.RosDomain;
import model.repositories.rosRepo.connection.ConnectionType;
import model.repositories.rosRepo.message.RosData;
import std_msgs.Bool;
import utility.Utils;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.1.3
 * @created on 10.01.20
 * @updated on 11.04.20
 * @modified by Nico Studt
 * @updated on 16.11.2020
 * @modified by Nils Rottmann
 */
public class MasterViewModel extends AndroidViewModel {

    private static final String TAG = MasterViewModel.class.getSimpleName();

    private final RosDomain rosDomain;

    private MutableLiveData<String> networkSSIDLiveData;
    private final LiveData<MasterEntity> currentMaster;


    public MasterViewModel(@NonNull Application application) {
        super(application);

        rosDomain = RosDomain.getInstance(application);
        currentMaster = rosDomain.getCurrentMaster();
    }


    public void setMasterIp(String ipString) {
        MasterEntity master = currentMaster.getValue();
        master.ip = ipString;
        rosDomain.updateMaster(master);
    }

    public void setMasterPort(String portString) {
        int port = Integer.parseInt(portString);
        MasterEntity master = currentMaster.getValue();
        master.port = port;
        rosDomain.updateMaster(master);
    }

    public void setMasterDeviceIp(String deviceIpString) {
        rosDomain.setMasterDeviceIp(deviceIpString);
    }

    public void connectToMaster() {
        rosDomain.connectToMaster();
    }

    public void disconnectFromMaster() {
        rosDomain.disconnectFromMaster();
    }

    public LiveData<MasterEntity> getMaster() {
        return rosDomain.getCurrentMaster();
    }

    public LiveData<ConnectionType> getRosConnection() {
        return rosDomain.getRosConnection();
    }

    public String setDeviceIp(String deviceIp){
        return deviceIp;
    }

    public LiveData<String> getCurrentNetworkSSID(){
        if (networkSSIDLiveData == null) {
            networkSSIDLiveData = new MutableLiveData<>();
        }

        setWifiSSID();

        return networkSSIDLiveData;
    }

    public ArrayList<String> getIPAddressList() {
        return Utils.getIPAddressList(true);
    }

    public String getIPAddress() {return Utils.getIPAddress(true); }

    private void setWifiSSID() {
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        String ssid = Utils.getWifiSSID(wifiManager);

        if (ssid == null) {
            ssid = "None";
        }

        networkSSIDLiveData.postValue(ssid);

    }

    public LiveData<RosData> getData(){
        return rosDomain.getData();
    }

    public Boolean getConnectedOrNot() {
        return rosDomain.getConnectedOrNot();
    }
}
