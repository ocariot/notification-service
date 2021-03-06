package notification_service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;


@Service
public class RabbitMQ{

    private static final Logger LOGGER = Logger.getLogger( RabbitMQ.class.getName() );

    @Autowired
    private MongoCollection<Document> collection;

    @Autowired
    private MongoCollection<Document> messagesCollection;

    @Autowired
    private MongoCollection<Document> pendingNotifications;

    @Autowired
    private FirebaseMessage firebaseMessage;

    @Autowired
    private RabbitMQRequester rabbitMQRequester;


    @RabbitListener(queues = "${rabbitmq.queue.send.notification}")
    public void notificationService(Message message) {

        try{

            byte[] body = message.getBody();
            JSONObject jsonmsg = new JSONObject(new String(body));

            if (jsonmsg.has("event_name")) {

                String eventName = (String) jsonmsg.get("event_name");

                switch (eventName){

                    case "SendNotificationEvent":

                        if (jsonmsg.has("notification")) {

                            JSONObject notificationJson = (JSONObject) jsonmsg.get("notification");

                            String messageType = notificationJson.getString("notification_type");

                            switch (messageType){

                                case "topic":

                                    try {
                                        String topic = notificationJson.getString("topic");
                                        String title = notificationJson.getString("title");
                                        String bod = notificationJson.getString("body");
                                        firebaseMessage.sendToTopic(topic, title, bod);
                                    } catch (JSONException e) {
                                        LOGGER.log(Level.WARNING, "Could not read topic,title or body from json message");
                                    } catch (FirebaseMessagingException e) {
                                        LOGGER.log(Level.WARNING, "Could not send topic notification.");
                                    }

                                    break;

                                case "mission:new":

                                case "mission:done":


                                    try{
                                        String uID = notificationJson.getString("id");
                                        firebaseMessage.sendToToken(uID,messageType,null, 0);

                                    } catch (JSONException e) {
                                        LOGGER.log(Level.WARNING, "Could not read id from json message");
                                    }
                                    break;

                                case "monitoring:miss_child_data":

                                    try {

                                        String username = null;
                                        String familyID;
                                        String educatorID;
                                        ArrayList<String> arrayEducators = new ArrayList<>();
                                        ArrayList<String> arrayFamilies = new ArrayList<>();
                                        int i;

                                        String uID = notificationJson.getString("id");
                                        int days_since = notificationJson.getInt("days_since");

                                        String info = rabbitMQRequester.send("?_id="+uID,"children.find");


                                        JSONArray jsonarray = new JSONArray(info);

                                        try {
                                            username = (String) jsonarray.getJSONObject(0).get("username");
                                        } catch (JSONException e) {
                                            LOGGER.log(Level.WARNING, "Could not fetch children username from account microservice");
                                        }

                                        ///notification for families
                                        String family = rabbitMQRequester.send("?children="+uID,"families.find");
                                        jsonarray = new JSONArray(family);
                                        try {
                                            for (i = 0; i < jsonarray.length(); i++) {

                                                familyID = (String) jsonarray.getJSONObject(i).get("id");
                                                if (familyID!=null && !familyID.isEmpty() && username!=null) {
                                                    firebaseMessage.sendToToken(familyID, messageType, username, days_since);

                                                }

                                            }

                                        } catch (JSONException e) {
                                            LOGGER.log(Level.WARNING, "Error processing family information for monitoring:miss_child_data notification");
                                        }


                                        ///notification for teacher
                                        String educators = rabbitMQRequester.send(uID,"child.educators.find");
                                        jsonarray = new JSONArray(educators);
                                        try {
                                            for (i = 0; i < jsonarray.length(); i++) {

                                                educatorID = (String) jsonarray.getJSONObject(i).get("id");
                                                if (educatorID!=null && !educatorID.isEmpty() && username!=null) {
                                                    firebaseMessage.sendToToken(educatorID, messageType, username, days_since);

                                                }

                                            }

                                        } catch (JSONException e) {
                                            LOGGER.log(Level.WARNING, "Error processing educator information for monitoring:miss_child_data notification");
                                        }


                                    } catch (JSONException e) {
                                        LOGGER.log(Level.WARNING, "Could not get id or days_since from json message.");
                                    }
                                    break;

                                case "iot:miss_data":

                                    try{

                                        String institutionID;

                                        institutionID = notificationJson.getString("institution_id");
                                        int days_since = notificationJson.getInt("days_since");
                                        JSONObject location = notificationJson.getJSONObject("location");
                                        String sensorType = notificationJson.getString("sensor_type");


                                        firebaseMessage.sendToToken(institutionID,messageType,sensorType,location,days_since);


                                    } catch (JSONException e) {
                                        LOGGER.log(Level.WARNING, "Could not get institution_id, sensor_type, location or days_since from json message.");
                                    }
                                    break;

                            }

                        }
                        break;

                    case "UserDeleteEvent":

                        String id;

                        if (jsonmsg.has("user")) {
                            try {

                                id = String.valueOf(jsonmsg.getJSONObject("user").get("id"));

                                Document doc = collection.find(eq("id", id)).first();

                                if (doc != null) {

                                    collection.deleteMany(doc);
                                    LOGGER.log(Level.INFO, "User " + id + " deleted from database");


                                } else {

                                    LOGGER.log(Level.WARNING, "User " + id + " does not exist on database");

                                }
                                } catch (JSONException e) {
                                    LOGGER.log(Level.WARNING, "An error occurred while attempting perform the operation with the UserDeleteEvent name event. Cannot read property 'id' or undefined");
                                }
                        }
                        break;
                }

            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "An error occurred while attempting to read message. Possible problem with JSON format");
        }

    }
}
