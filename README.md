# Microservice

## Introduction

This project consists to two microservices, **Publisher Service** and **Subscriber Service**.

**Publisher Service**: This service collects inputs from the publisher API and persists the input in cassandra `publisher` table. The publisher API after persisting the data in cassandra, reads the publisher's name (pub_name) from the cassandra publisher table and writes it to a kafka topic.

**Subscriber Service**: This service reads message from the kafka topic and persists the same in cassandra `subscriber` table.

## Steps to use publisher and subscriber services:

1. setup cassandra (create keyspace)
2. setup kafka (create kafka topic)
3. Update the properties in default.properties file for the publisher and subscriber services.
4. Run publisher and subscriber services using ./gradlew clean run
5. Execute tables api to create tables in cassandra 
    
    Raw text: 
    >[Note: update the server_port]
    
    ```
    curl --location --request POST ‘http://localhost:<server_port>/cassandra/tables’
    ```
    
6. Execute publisher api and wait for the magic to happen
    
    
    Raw text: 
    >[Note: update server_port and provide values for pub_id and pub_name]
    
    ```
    curl --location --request POST 'http://localhost:<server_port>/cassandra/publisher' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "pub_id":<int value>,
        "pub_name":<string value>
    }'
    ```


## Example

**properties:**

server_port=8080

cassandra_host=127.0.0.1
cassandra_port=9042
cassandra_keyspace=microservice

kafka_host=localhost
kafka_port=9092
kafka_topic=microservice

1. ```
    curl --location --request POST ‘http://localhost:8080/cassandra/tables’
    ```

    On importing the above raw text in Postman and running the `tables API`, two tables gets created in cassandra (under keyspace provided in properties) as follows:
    
    publisher table:
    
    |pub_id|pub_name|
    |------|--------|
    |       |       |
    
    subscriber table:
    
    |sub_no|pub_name|sub_name|
    |------|--------|--------|
    |       |       |        |
    
2.  ```
    curl --location --request POST 'http://localhost:8080/cassandra/publisher' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "pub_id":1,
        "pub_name":"Sweta"
    }'
    ```
    
    On importing the above raw text in Postman and running the `publisher API`,
    
    **The publisher service does the below:**
    
    - The inputs gets persisted in cassandra publisher table
    
    
    |pub_id|pub_name|
    |------|--------|
    |1|Sweta|
    
    - pub_name is written to kafka topic
    
    ```
    ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic microservice
  
    Sweta
    ```
    
    **The subscriber service does the below:**
    
    - read from the kafka topic and persists the data in cassandra subscriber table.
    
    |sub_no|pub_name|sub_name|
    |------|--------|--------|
    |0|Sweta|Hello|
    
