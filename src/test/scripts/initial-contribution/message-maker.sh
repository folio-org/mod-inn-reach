FILE="$1"
for num in {1..200}
do
  echo '2d776536-9045-11ed-a1eb-0242ac120002#{ "jobId": "2d776536-9045-11ed-a1eb-0242ac120002", "type": "UPDATE", "tenant": "diku", "instanceId": "2d776536-9045-11ed-a1eb-0242ac120002" }' >> "$FILE"
done
echo "appended $num messages to $FILE"
echo "file has this many lines: $(wc -l "$FILE")"
