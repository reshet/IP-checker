Application for checking of client's IPs in local network via UDP broadcast.

Do `mvn clean install`.

Make run scripts executable:

  chmod +x server.sh
  chmod +x client.sh

Usage:

 - to run as client, execute `./client.sh <nikename>`
 - to run as server, execute `./server.sh`

 The server will listen to all incoming requests on port 5005,
 and print out the map of user nikenames and their current IP addresses.

 Client will send the probe to server via bradcast with it's nikename each 10 seconds.