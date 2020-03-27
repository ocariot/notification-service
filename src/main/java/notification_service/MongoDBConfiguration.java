package notification_service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



@Configuration
public class MongoDBConfiguration{

    @Value("${spring.mongodb.uri}")
    public String mongoURI;
    @Value("${spring.mongodb.database}")
    public String mongoDatabase;
    @Value("${spring.mongodb.collection}")
    public String mongoCollection;
    @Value("${server.ssl.key-store}")
    public String keystorePath;
    @Value("${server.ssl.key-store-password}")
    public String keystorePass;
    @Value("${server.ssl.key-truststore}")
    public String truststorePath;


    @Bean
    public MongoDatabase database() {

        System.setProperty ("javax.net.ssl.keyStore",keystorePath);
        System.setProperty ("javax.net.ssl.keyStorePassword",keystorePass);
        System.setProperty ("javax.net.ssl.trustStore",truststorePath);
        System.setProperty ("javax.net.ssl.trustStorePassword","changeit");
        MongoClient mongoClient = MongoClients.create(mongoURI+"&sslInvalidHostNameAllowed=true");
        MongoDatabase database = mongoClient.getDatabase(mongoDatabase);
        return database;
    }


    @Bean
    public MongoCollection<Document> collection(MongoDatabase database) {


        return database.getCollection("users");

    }


    @Bean
    public MongoCollection<Document> messagesCollection(MongoDatabase database) {

        List<Document> documents = new ArrayList<Document>();
        try{


            JSONParser jsonParser = new JSONParser();
            FileReader reader = new FileReader("/Users/jpdoliveira/ocariot/docker-compose-master/messages.json");
            documents= (List<Document>) jsonParser.parse(reader);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        database.getCollection("messages").deleteMany(new Document());
        database.getCollection("messages").insertMany(documents);

        return database.getCollection("messages");

    }

    @Bean
    public MongoCollection<Document> pendingNotifications(MongoDatabase database) {


        return database.getCollection("pendingNotifications");

    }
}
