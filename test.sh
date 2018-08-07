NUM=$1
echo $NUM

i="0"
while [ $i -lt $NUM ] ; do
  curl "http://localhost:8081/doordash/order/${i}?customer=foo&foodItem=bar" &
  i=$[$i+1]
done
