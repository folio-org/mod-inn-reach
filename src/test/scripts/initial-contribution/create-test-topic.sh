# Copy this into a directory where you have the kafka console command line tools installed.
# Also need to copy that locally.
TEST=$1

echo "creating topic $TEST...T"
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic folio.contrib.tester."$TEST"
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
echo "adding messages..."
./bin/kafka-console-producer.sh --broker-list localhost:9092 --topic folio.contrib.tester."$TEST" < messages.txt --property "parse.key=true" --property "key.separator=#"
echo "resetting offsets..."
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group folio-mod-innreach-contribution-events-group --topic folio.contrib.tester."$TEST":0 --to-offset 0 --reset-offsets --execute
