LOG_FILE=/tmp/tracing.log
rm -f $LOG_FILE

java -jar doordash/target/doordash-1.0-SNAPSHOT.jar server doordash/app.yaml >> $LOG_FILE 2>&1 &
java -jar restaurant/target/restaurant-1.0-SNAPSHOT.jar server restaurant/app.yaml >> $LOG_FILE 2>&1 &
java -jar dasher/target/dasher-1.0-SNAPSHOT.jar server dasher/app.yaml >> $LOG_FILE 2>&1 &
java -jar restaurant/target/restaurant-1.0-SNAPSHOT.jar server restaurant/app.yaml >> $LOG_FILE 2>&1 &
