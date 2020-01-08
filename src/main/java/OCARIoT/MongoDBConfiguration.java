package OCARIoT;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MongoDBConfiguration{

    @Value("${spring.data.mongodb.uri}")
    public String mongoURI;
    @Value("${spring.data.mongodb.database}")
    public String mongoDatabase;
    @Value("${spring.data.mongodb.collection}")
    public String mongoCollection;

    @Bean
    public MongoCollection<Document> collection() {
        final MongoClient mongoClient = MongoClients.create(mongoURI);
        final MongoDatabase database = mongoClient.getDatabase(mongoDatabase);
        return database.getCollection(mongoCollection);
    }
}
