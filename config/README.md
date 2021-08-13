This folder contains configurations for three nodes for local testing with Kafka backend. Using the values.yml for each node, we can start three instances in IDEs to debug and test for the interactions between instances locally.

start the node1

```
cd ~/networknt/light-controller
java -jar -Dlight-4j-config-dir=config/node1 target/controller.jar
```

start the node2

```
cd ~/networknt/light-controller
java -jar -Dlight-4j-config-dir=config/node2 target/controller.jar

```

start the node3

```
cd ~/networknt/light-controller
java -jar -Dlight-4j-config-dir=config/node3 target/controller.jar

```