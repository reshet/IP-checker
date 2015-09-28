package com.enterprise.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple app designed to track IPs of users in local network.
 * Some code examples are taken from here: http://michieldemey.be/blog/network-discovery-using-udp-broadcast/
 */

public class App {


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: java -jar ipchecker.jar <client [username] | server>");
            System.exit(1);
        }

        String mode = args[0];
        if ("server".equals(mode)) {
            runserver();
        } else {
            if (args.length == 2) {
            runclient(args[1]);
            }
        }

    }

    private static void runclient(String username) {
        System.out.println("Run in client mode");
        while(true) {

            // Find the server using UDP broadcast
            try {
                //Open a random port to send the package
                DatagramSocket c = new DatagramSocket();
                c.setBroadcast(true);
                c.setSoTimeout(5*1000);

                byte[] sendData = ("IPTEST_" + username).getBytes();

                //Try the 255.255.255.255 first
                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), serverPort);
                    c.send(sendPacket);
                    System.out.println("--> Request packet sent to: 255.255.255.255 (DEFAULT)");
                } catch (Exception e) {
                }

                // Broadcast the message over all the network interfaces
                Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = (NetworkInterface)interfaces.nextElement();

                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue; // Don't want to broadcast to the loopback interface
                    }

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }

                        // Send the broadcast package!
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, serverPort);
                            c.send(sendPacket);
                        } catch (Exception e) {
                        }

                        System.out.println("--> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                    }
                }

                System.out.println("--> Done looping over all network interfaces. Now waiting for a reply!");

                //Wait for a response
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

                c.receive(receivePacket);

                //We have a response
                System.out.println("--> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

                //Check if the message is correct
                String message = new String(receivePacket.getData()).trim();
                if (message.equals("IPTEST_DONE")) {
                    System.out.println("IP test done. Thank you, " + username + "!");
                }

                //Close the port!
                c.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static int serverPort = 5005;
    private static void runserver()  {
        System.out.println("Run in server mode.");
        Map<String, String> mapOfIps = new HashMap<String, String>();
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(serverPort, InetAddress.getByName("0.0.0.0"));
            serverSocket.setBroadcast(true);

            SocketAddress inetAddress = serverSocket.getLocalSocketAddress();
            System.out.println("--> Server address " + inetAddress + " , port " + serverPort);

            while(true) {

                byte [] recieveBuffer = new byte[20000];
                DatagramPacket packet = new DatagramPacket(recieveBuffer, recieveBuffer.length);

                serverSocket.receive(packet);
                System.out.println("--> Packet received from: " + packet.getAddress().getHostAddress());
                System.out.println("--> Packet data: " + new String(packet.getData()));

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.startsWith("IPTEST_")) {
                    String username = message.split("_")[1];
                    mapOfIps.put(username, packet.getAddress().getHostAddress());
                    byte[] sendData = "IPTEST_DONE".getBytes();
                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    serverSocket.send(sendPacket);
                    System.out.println("--> Sent packet to: " + sendPacket.getAddress().getHostAddress());
                    System.out.println("Mapping:");
                    System.out.println(mapOfIps);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
          if (serverSocket != null) {
            serverSocket.close();
          }
        }
    }
}
