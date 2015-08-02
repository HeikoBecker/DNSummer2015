rm output.txt
echo "CSMA ALOHA 0.35" >> output.txt
modes csma-aloha.modest -E "PACKETS=100,P=0.35" --max-run-length 100000 >> output.txt
echo "CSMA ALOHA 0.25" >> output.txt
modes csma-aloha.modest -E "PACKETS=100,P=0.25" --max-run-length 100000 >> output.txt
echo "CSMA ALOHA 0.15" >> output.txt
modes csma-aloha.modest -E "PACKETS=100,P=0.15" --max-run-length 100000 >> output.txt
echo "CSMA/CD ALOHA 0.35" >> output.txt
modes csmacd-aloha.modest -E "PACKETS=100,P=0.35" --max-run-length 100000 >> output.txt
echo "CSMA/CD ALOHA 0.25" >> output.txt
modes csmacd-aloha.modest -E "PACKETS=100,P=0.25" --max-run-length 100000 >> output.txt
echo "CSMA/CD ALOHA 0.15" >> output.txt
modes csmacd-aloha.modest -E "PACKETS=100,P=0.15" --max-run-length 100000 >> output.txt

echo "CSMA BEB" >> output.txt
modes csma-beb.modest -E "PACKETS=100" --max-run-length 100000 >> output.txt
echo "CSMA/CD BEB" >> output.txt
modes csmacd-beb.modest -E "PACKETS=100" --max-run-length 100000 >> output.txt