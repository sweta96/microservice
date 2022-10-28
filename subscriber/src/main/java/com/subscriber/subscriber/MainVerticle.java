package com.subscriber.subscriber;

import com.datastax.driver.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.kafka.client.consumer.KafkaConsumer;

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

    // creating kafka consumer configurations
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", kafka_contact_points);
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", "subscriber");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");

    // creating kafka consumer using the configurations set
    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);

    // subscrining kafka consumer to the kafka topic: publisher
    consumer.subscribe(kafka_topic);

    // creating connection with cassandra
    Cluster cluster = Cluster.builder().addContactPoint(cassandra_host).withPort(Integer.parseInt(cassandra_port)).build();
    Session session = cluster.connect(cassandra_keyspace);

    // the consumer handler is building and executing query to write data read from kafka topic to cassandra table subscriber
    consumer.handler(record -> {
      String query = "INSERT INTO subscriber (sub_no, sub_name, pub_name) VALUES ("+record.offset()+",'Hello','"+record.value()+"');";
      session.execute(query);
    });

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
