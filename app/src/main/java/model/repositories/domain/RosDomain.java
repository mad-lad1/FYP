package model.repositories.domain;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;



import java.util.List;

import model.entities.BaseEntity;
import model.entities.MasterEntity;
import model.repositories.ConfigRepository;
import model.repositories.ConfigRepositoryImpl;
import model.repositories.rosRepo.RosRepository;
import model.repositories.rosRepo.connection.ConnectionType;
import model.repositories.rosRepo.message.RosData;
import model.repositories.rosRepo.message.Topic;
import model.repositories.rosRepo.node.BaseData;
import utility.LambdaTask;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.3
 * @created on 07.04.20
 * @updated on 15.04.20
 * @modified by Nico Studt
 * @updated on 15.05.20
 * @modified by Nico Studt
 * @updated on 27.07.20
 * @modified by Nils Rottmann
 * @updated on 16.11.20
 * @modified by Nils Rottmann
 */
public class RosDomain {

    private static final String TAG = RosDomain.class.getSimpleName();

    // Singleton instance
    private static RosDomain mInstance;

    // Repositories
    private final ConfigRepository configRepository;
    private final RosRepository rosRepo;

    // Data objects
    private final LiveData<List<BaseEntity>> currentWidgets;
    private final LiveData<MasterEntity> currentMaster;

    private RosDomain(@NonNull Application application) {

        this.rosRepo = RosRepository.getInstance(application);
        this.configRepository = ConfigRepositoryImpl.getInstance(application);

        // React on config change and get the new data
        currentWidgets = Transformations.switchMap(configRepository.getCurrentConfigId(),
                configId -> configRepository.getWidgets(configId));

        currentMaster = Transformations.switchMap(configRepository.getCurrentConfigId(),
                configId -> configRepository.getMaster(configId));

        currentWidgets.observeForever(widgets -> rosRepo.updateWidgets(widgets));
        currentMaster.observeForever(master -> rosRepo.updateMaster(master));
    }


    public static RosDomain getInstance(Application application) {
        if (mInstance == null) {
            mInstance = new RosDomain(application);
        }

        return mInstance;
    }

    public void publishData(BaseData data) {
        rosRepo.publishData(data);
    }

    public void createWidget(String widgetType) {
        new LambdaTask(() -> configRepository.createWidget(widgetType)).execute();
    }

    public void addWidget(BaseEntity widget) {
        configRepository.addWidget(widget);
    }

    public void updateWidget(BaseEntity widget) {
        configRepository.updateWidget(widget);
    }

    public void deleteWidget(BaseEntity widget) {
        configRepository.deleteWidget(widget);
    }

    public LiveData<List<BaseEntity>> getCurrentWidgets() {
        return this.currentWidgets;
    }

    public LiveData<RosData> getData(){ return this.rosRepo.getData(); }


    public void updateMaster(MasterEntity master) {
        configRepository.updateMaster(master);
    }

    public void setMasterDeviceIp(String deviceIp) {rosRepo.setMasterDeviceIp(deviceIp); }

    public void connectToMaster() {
        rosRepo.connectToMaster();
    }

    public void disconnectFromMaster() {
        rosRepo.disconnectFromMaster();
    }

    public LiveData<MasterEntity> getCurrentMaster() {
        return this.currentMaster;
    }

    public LiveData<ConnectionType> getRosConnection() {
        return rosRepo.getRosConnectionStatus();
    }

    public List<Topic> getTopicList() { return rosRepo.getTopicList(); }

    public Boolean getConnectedOrNot() {
        return rosRepo.getConnectedOrNot();
    }
}
