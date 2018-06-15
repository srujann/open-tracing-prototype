# Summary
This is a prototype to integrate Dropwizard and gRPC apps with Jaeger.
<br />
Distributed traces are sent from different apps across process boundaries for your end to end request <-> response.

# Steps to run this manually

* Make sure Jaeger is running on your env using the Docker image - https://www.jaegertracing.io/docs/getting-started/
* Clone the repo
  * Make sure you have Java and Maven are installed on your machine.
* Build the source code to generate the JAR - `mvn clean install -DskipTests` is installed.
  * You will see 3 jars built - 
    * doordash/target/doordash-1.0-SNAPSHOT.jar
    * dasher/target/dasher-1.0-SNAPSHOT.jar
    * restaurant/target/restaurant-1.0-SNAPSHOT.jar
* Run the 3 Dropwizard apps that talk to each other
  * java -jar doordash/target/doordash-1.0-SNAPSHOT.jar server
  * java -jar restaurant/target/restaurant-1.0-SNAPSHOT.jar server
  * java -jar dasher/target/dasher-1.0-SNAPSHOT.jar server
* Now issue an HTTP GET request - http://localhost:8081/doordash/order?customer=foo&foodItem=bar
  * Sample Response - Yay!!! Ordered and Delivered - foodItem: bar for customer: foo
* Now go to Jaeger UI and look for traces for service "doordash" and click on Find Traces.
  * You should see 5 spans for your latest HTTP request. Zoom into individual span for more information on how the apps talk to each other.
