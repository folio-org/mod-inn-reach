TEST=$1
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group folio-mod-innreach-contribution-events-group --topic folio.contrib.tester."$TEST":0 --to-offset 0 --reset-offsets --execute
