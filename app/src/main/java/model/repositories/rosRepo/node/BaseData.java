package model.repositories.rosRepo.node;



import org.ros.internal.message.Message;
import org.ros.node.topic.Publisher;

import model.entities.BaseEntity;
import model.repositories.rosRepo.message.Topic;


public abstract class BaseData {

    protected Topic topic;


    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public Message toRosMessage(Publisher<Message> publisher, BaseEntity widget){
        return null;
    }
}
