# Microservice

## Steps to use publisher and subscriber services:

1. setup cassandra (create keyspace)
2. setup kafka (create kafka topic)
3. Update the properties in default.properties file for the publisher and subscriber services.
4. Run publisher and subscriber services using ./gradlew clean run
5. Execute tables api to create tables in cassandra 
    
    Raw text: 
    >[Note: update the server_port]
    
    `curl --location --request POST ‘http://localhost:<server_port>/cassandra/tables’`
6. Execute publisher api and wait for the magic to happen
    
    
    Raw text: 
    >[Note: update server_port and provide values for pub_id and pub_name]
    
    
    `curl --location --request POST 'http://localhost:<server_port>/cassandra/publisher' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "pub_id":<int value>,
        "pub_name":<string value>
    }'`
