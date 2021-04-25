package model.repositories;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import model.repositories.rosRepo.MasterViewModel;
import model.repositories.rosRepo.connection.ConnectionType;
import sensor_msgs.Image;
import std_msgs.Float32MultiArray;

public class MasterConnection {


    private MasterViewModel masterViewModel;
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private String deviceIP;
    private ViewModelStoreOwner owner;


    public MasterConnection(Context context, LifecycleOwner lifecycleOwner, String deviceIP, ViewModelStoreOwner viewModelStoreOwner){
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.deviceIP = deviceIP;
        this.owner = viewModelStoreOwner;
    }

    public void establishMasterConnection(){
        masterViewModel = new ViewModelProvider(owner).get(MasterViewModel.class);
        masterViewModel.getRosConnection().observe(lifecycleOwner,
               this::setRosConnection );

        masterViewModel.setMasterDeviceIp(deviceIP);
        masterViewModel.connectToMaster();

    }

    public Float32MultiArray getData(){
         return (Float32MultiArray) masterViewModel.getData().getValue().getMessage();
    }

    private void setRosConnection(ConnectionType connectionType){

    }

    public Boolean getConnectedOrNot(){
        return masterViewModel.getConnectedOrNot();
    }




}
