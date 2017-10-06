package de.re.eeip.discoverdevices;

import java.io.Console;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.*;
import java.util.*;

/**
 * Created by sr555 on 26.09.17.
 */
public class DiscoverDevices extends Thread {

    private  List<de.re.eeip.encapsulation.CipIdentityItem> returnValue = new ArrayList<>();

    public List<de.re.eeip.encapsulation.CipIdentityItem> ListIdentity() {
        DatagramSocket socket = null;
        boolean running;
        byte[] buf = new byte[256];
        int outputPort = 0;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (IOException e) {}



                try {

                    socket = new DatagramSocket();
                    socket.setReuseAddress(true);
                    byte[] sendData = new byte[24];
                    sendData[0] = 0x63;               //Command for "ListIdentity"
                    outputPort = socket.getLocalPort();

                    DatagramPacket packet
                            = new DatagramPacket(sendData, sendData.length, InetAddress.getByName( "255.255.255.255" ), 44818);



                    socket.send(packet);


                }
                catch (IOException  e){}


                socket.close();
                socket.disconnect();
                ListenerThread listenerThread = new ListenerThread(outputPort);
                listenerThread.start();



        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return returnValue;
                }

    class ListenerThread extends Thread
    {
        byte[] buf = new byte[1024];
        DatagramSocket socket;
        private int outputPort;

        public  ListenerThread(int outputPort)
        {
            this.outputPort = outputPort;
        }


        public void run()
        {
            try {
                socket = new DatagramSocket(outputPort);
                socket.setReuseAddress(true);
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.setSoTimeout(2000);
                while (true) {
                    socket.receive(packet);


                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    byte[] receivedBytes = packet.getData();
                    String received
                            = new String(receivedBytes, 0, packet.getLength());
                    //System.out.println(received);
                    returnValue.add((new de.re.eeip.encapsulation.CipIdentityItem()).getCIPIdentityItem(24, receivedBytes));
                }
            }
            catch (IOException e){

            }
            finally
            {
                socket.close();
                socket.disconnect();

            }


        }
    }

    }



