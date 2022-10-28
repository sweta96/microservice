package com.publisher.publisher;

import com.datastax.driver.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    //setting up properties
    Properties prop = readPropertiesFile("default.properties");
    String cassandra_host = prop.getProperty("cassandra_host");
    String cassandra_port = prop.getProperty("cassandra_port");
    String cassandra_keyspace = prop.getProperty("cassandra_keyspace");
    String kafka_contact_points = prop.getProperty("kafka_host")+":"+prop.getProperty("kafka_port");
    String kafka_topic = prop.getProperty("kafka_topic");
    String server_port = prop.getProperty("server_port");

    // Creating server and router for http request
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);

    // creating connection with cassandra
    Cluster cluster = Cluster.builder().addContactPoint(cassandra_host).withPort(Integer.parseInt(cassandra_port)).build();
    Session session = cluster.connect(cassandra_keyspace);

    // creating kafka producer configurations
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", kafka_contact_points);
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");

    // creating kafka producer using the configurations set
    KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config);

    // creating a route for post api call, which will read a json and persist the value into cassandra.
    // After persisting the value in cassandra, it will read the value from cassandra and write it to the kafka topic
    router.post("/cassandra/publisher").handler( ctx -> {
       ctx.request().bodyHandler( body -> {
         // reading the body as a json object
         JsonObject result = (JsonObject) body.toJson();
         // building and executing query to persist data in cassandra table publisher
         String query = "INSERT INTO publisher (pub_id, pub_name) VALUES ("+result.getString("pub_id")+",'"+result.getString("pub_name")+"');";
         session.execute(query);

         // building and executing query to read data from cassandra
         String kquery = "SELECT pub_name from publisher where pub_id="+result.getString("pub_id")+";";
         ResultSet names = session.execute(kquery);

         // writing the data read from cassandra to kafka topic
         for (Row row : names) {
           String value = row.getString(0);
           KafkaProducerRecord<String, String> record =
             KafkaProducerRecord.create(kafka_topic, value);
           producer.write(record);
        }
       });
       ctx.response().end("Message has been persisted in Cassandra and written to kafka topic!");
      });

    router.post("/cassandra/tables").handler( ctx -> {

      // building and executing query to create publisher table
      String query = "CREATE TABLE publisher(pub_id int PRIMARY KEY, pub_name text);";
      session.execute(query);

      // building and executing query to create subscriber table
      String query1 = "CREATE TABLE subscriber(sub_no int PRIMARY KEY, sub_name text, pub_name text);";
      ResultSet names = session.execute(query1);

      ctx.response().end("Cassandra publisher and subscriber tables created!");
    });

    router.route().handler(ctx -> {

      // This handler will be called for every request
      HttpServerResponse response = ctx.response();
      response.putHeader("content-type", "text/plain");

      // Write to the response and end it
      response.end("Welcome to Publsher Mircoservice");
    });

    server.requestHandler(router).listen(Integer.parseInt(server_port));
  }

  public static Properties readPropertiesFile(String fileName) throws IOException {
    FileInputStream fis = null;
    Properties prop = null;
    try {
      fis = new FileInputStream(fileName);
      prop = new Properties();
      prop.load(fis);
    } catch(FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    } catch(IOException ioe) {
      ioe.printStackTrace();
    } finally {
      fis.close();
    }
    return prop;
  }
}
