package de.re.eeip;

import com.sun.corba.se.pept.transport.Acceptor;
import com.sun.corba.se.pept.transport.ListenerThread;
import de.re.eeip.cip.datatypes.CIPCommonServicesEnum;
import de.re.eeip.cip.datatypes.ConnectionType;
import de.re.eeip.cip.datatypes.RealTimeFormat;
import de.re.eeip.cip.exception.CIPException;
import de.re.eeip.discoverdevices.DiscoverDevices;
import de.re.eeip.encapsulation.CipIdentityItem;
import de.re.eeip.encapsulation.datatypes.CommandsEnum;

import java.io.*;
import java.net.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.MulticastChannel;
import java.nio.charset.Charset;
import java.util.List;


/**
 * Created by sr555 on 26.09.17.
 */
public class EEIPClient
{
    private int sessionHandle;
    private String ipAddress;
    private int tcpPort = 44818;
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private DataInputStream inFromServer;
    private UdpListenerThread listenerThread;
    private UdpSendThread sendThread;
    private int connectionSerialNumber;
    private int connectionID_O_T;
    private int connectionID_T_O;
    private RealTimeFormat o_t_realTimeFormat = RealTimeFormat.Header32Bit;
    private RealTimeFormat t_o_realTimeFormat = RealTimeFormat.Modeless;
    private ConnectionType o_t_connectionType = ConnectionType.Point_to_Point;
    private ConnectionType t_o_connectionType = ConnectionType.Multicast;
    private long requestedPacketRate_O_T  = 0x7A120;      //500ms
    private long requestedPacketRate_T_O  = 0x7A120;      //500ms
    private boolean o_t_ownerRedundant  = true;                //For Forward Open
    private boolean t_o_ownerRedundant  = true;                //For Forward Open
    private de.re.eeip.cip.datatypes.Priority o_t_priority = de.re.eeip.cip.datatypes.Priority.Scheduled;
    private de.re.eeip.cip.datatypes.Priority t_o_priority = de.re.eeip.cip.datatypes.Priority.Scheduled;
    private boolean o_t_variableLength = true;                //For Forward Open
    private boolean t_o_variableLength = true;                //For Forward Open
    private int o_t_length  = 505;                //For Forward Open - Max 505
    private int t_o_length  = 505;                //For Forward Open - Max 505
    private int o_t_instanceID  = 0x64;               //Ausgänge
    private int t_o_instanceID = 0x65;               //Eingänge
    public byte[] O_T_IOData = new byte[505];   //Class 1 Real-Time IO-Data O->T
    public byte[] T_O_IOData = new byte[505];    //Class 1 Real-Time IO-Data T->O


    private byte assemblyObjectClass  = 0x04;
    private int originatorUDPPort = 0x08AE;
    private int targetUDPPort = 0x08AE;
    private long multicastAddress;


    private de.re.eeip.objectlibrary.AssemblyObject assemblyObject;
    /**
     * Implementation of the Assembly Object (Class Code: 0x04)
     * @return  assembly object
     */
    @SuppressWarnings("unused")
    public de.re.eeip.objectlibrary.AssemblyObject getAssemblyObject()
    {
        if (assemblyObject == null)
            assemblyObject = new de.re.eeip.objectlibrary.AssemblyObject(this);
        return assemblyObject;
    }


    /**
     * List and identify potential targets. This command shall be sent as braodcast message using UDP.
     * @return Found Ethernet/IP Devices
     */
    public List<CipIdentityItem> ListIdentity() {
        de.re.eeip.discoverdevices.DiscoverDevices discoverDevices = new de.re.eeip.discoverdevices.DiscoverDevices();

            return discoverDevices.ListIdentity();
    }


    /**
     * Sends a RegisterSession command to a target to initiate session
     * @param address   IP-Address of the target device
     * @param port      Port of the target device (default should be 0xAF12)
     * @return  Session Handle
     */
    public int RegisterSession(int address, int port) throws IOException {
        if (sessionHandle != 0)
            return sessionHandle;
        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.Command = de.re.eeip.encapsulation.datatypes.CommandsEnum.RegisterSession;
        encapsulation.Length = 4;
        encapsulation.CommandSpecificData.add((byte)1);       //Protocol version (should be set to 1)
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);       //Session options shall be set to "0"
        encapsulation.CommandSpecificData.add((byte)0);


        String ipAddress = de.re.eeip.encapsulation.CipIdentityItem.getIPAddress(address);
        this.ipAddress = ipAddress;

        clientSocket = new Socket(ipAddress, port);
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new DataInputStream(clientSocket.getInputStream());
        outToServer.write(encapsulation.toBytes());

        byte[] data = new byte[256];
        inFromServer.read(data);

        int returnvalue = (0xff & data[4]) + ((0xff & data[5]) << 8) + ((0xff & data[6]) << 16) + ((0xff & data[7]) << 24);
        this.sessionHandle = returnvalue;
        return returnvalue;
    }

    /**
     * Sends a RegisterSession command to a target to initiate session
     * @param address   IP-Address of the target device
     * @param port      Port of the target device (default should be 0xAF12)
     * @return  Session Handle
     */
    public int RegisterSession(String address, int port) throws IOException {
        this.ipAddress = address;

        String[] addressSubstring = address.split("\\.");
        int addressByte0 = Integer.parseInt(addressSubstring[0]);
        int addressByte1 = Integer.parseInt(addressSubstring[1]);
        int addressByte2 = Integer.parseInt(addressSubstring[2]);
        int addressByte3 = Integer.parseInt(addressSubstring[3]);

        int ipAddress = (0xff & addressByte3) + (((0xff & addressByte2)) << 8) + ((0xff & addressByte1) << 16) + ((0xff & addressByte0) << 24);
        return RegisterSession(ipAddress, port);
    }

    /**
     * Sends a RegisterSession command to a target to initiate session with the Standard or predefined Port (Standard: 0xAF12)
     * @param address   IP-Address of the target device
     * @return          Session Handle
     */
    public int RegisterSession(String address) throws IOException {
        this.ipAddress = address;
        String[] addressSubstring = address.split("\\.");
        int addressByte0 = Integer.parseInt(addressSubstring[0]);
        int addressByte1 = Integer.parseInt(addressSubstring[1]);
        int addressByte2 = Integer.parseInt(addressSubstring[2]);
        int addressByte3 = Integer.parseInt(addressSubstring[3]);

        int ipAddress = (0xff & addressByte3) + (((0xff & addressByte2)) << 8) + ((0xff & addressByte1) << 16) + ((0xff & addressByte0) << 24);
        return RegisterSession(ipAddress, tcpPort);

    }

    /**
     * Sends a RegisterSession command to a target to initiate session with the Standard or predefined Port and Predefined IPAddress (Standard-Port: 0xAF12)
     * @return  Session Handle
     */
    public int RegisterSession() throws IOException {

        return RegisterSession(this.ipAddress, this.tcpPort);
    }

    /**
     * Sends a UnRegisterSession command to a target to terminate session
     */
    public void UnRegisterSession() throws IOException {
        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.Command = CommandsEnum.UnRegisterSession;
        encapsulation.Length = 0;
        encapsulation.SessionHandle =  sessionHandle;
        outToServer.write(encapsulation.toBytes());

        clientSocket.close();
        outToServer.close();
        inFromServer.close();

        sessionHandle = 0;
    }

    /**
     * Implementation of Common Service "Get_Attribute_Single"
     * @param classID       Class id of requested Attributes
     * @param instanceID    Instance of requested Attributes
     * @param attributeID   Attributes of requested Attributes
     * @return              Session Handle
     */
    public byte[] GetAttributeSingle(int classID, int instanceID, int attributeID) throws CIPException, IOException {
        if (sessionHandle == 0)             //If a Session is not Registers, Try to Registers a Session with the predefined IP-Address and Port
            this.RegisterSession();
        byte[] dataToSend = new byte[48];
        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.SessionHandle = sessionHandle;
        encapsulation.Command = CommandsEnum.SendRRData;
        encapsulation.Length = 24;
        //---------------Interface Handle CIP
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Interface Handle CIP

        //----------------Timeout
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Timeout

        //Common Packet Format (Table 2-6.1)
        de.re.eeip.encapsulation.datatypes.CommonPacketFormat commonPacketFormat = new  de.re.eeip.encapsulation.datatypes.CommonPacketFormat();
        commonPacketFormat.ItemCount = 0x02;

        commonPacketFormat.AddressItem = 0x0000;        //NULL (used for UCMM Messages)
        commonPacketFormat.AddressLength = 0x0000;

        commonPacketFormat.DataItem = 0xB2;
        commonPacketFormat.DataLength = 8;



        //----------------CIP Command "Get Attribute Single"
        commonPacketFormat.Data.add((byte) CIPCommonServicesEnum.Get_Attribute_Single.getValue());
        //----------------CIP Command "Get Attribute Single"

        //----------------Requested Path size
        commonPacketFormat.Data.add((byte)3);
        //----------------Requested Path size

        //----------------Path segment for Class ID
        commonPacketFormat.Data.add((byte)0x20);
        commonPacketFormat.Data.add((byte)classID);
        //----------------Path segment for Class ID

        //----------------Path segment for Instance ID
        commonPacketFormat.Data.add((byte)0x24);
        commonPacketFormat.Data.add((byte)instanceID);
        //----------------Path segment for Instace ID

        //----------------Path segment for Attribute ID
        commonPacketFormat.Data.add((byte)0x30);
        commonPacketFormat.Data.add((byte)attributeID);
        //----------------Path segment for Attribute ID

        byte[] dataToWrite = new byte[encapsulation.toBytes().length + commonPacketFormat.toBytes().length];
        System.arraycopy(encapsulation.toBytes(), 0, dataToWrite, 0, encapsulation.toBytes().length);
        System.arraycopy(commonPacketFormat.toBytes(), 0, dataToWrite, encapsulation.toBytes().length, commonPacketFormat.toBytes().length);

        outToServer.write(dataToWrite);

        byte[] data = new byte[564];
        int bytes = inFromServer.read(data);


        //--------------------------BEGIN Error?
        if (data[42] != 0)      //Exception codes see "Table B-1.1 CIP General Status Codes"
        {
            throw new de.re.eeip.cip.exception.CIPException(de.re.eeip.cip.datatypes.GeneralStatusCodes.GetStatusCode(data[42]));
        }
        //--------------------------END Error?

        byte[] returnData = new byte[bytes - 44];
        System.arraycopy(data, 44, returnData, 0, bytes-44);

        return returnData;
    }

    /**
     * Implementation of Common Service "Get_Attribute_All" - Service Code: 0x01
     * @param classID       Class id of requested Attributes
     * @param instanceID    Instance of requested Attributes
     * @return              Session Handle
     */
    public byte[] GetAttributeAll(int classID, int instanceID)  throws CIPException, IOException
    {
        if (sessionHandle == 0)             //If a Session is not Registered, Try to Registers a Session with the predefined IP-Address and Port
            this.RegisterSession();
        byte[] dataToSend = new byte[46];
        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.SessionHandle = sessionHandle;
        encapsulation.Command = CommandsEnum.SendRRData;
        encapsulation.Length = 22;
        //---------------Interface Handle CIP
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Interface Handle CIP

        //----------------Timeout
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Timeout

        //Common Packet Format (Table 2-6.1)
        de.re.eeip.encapsulation.datatypes.CommonPacketFormat commonPacketFormat = new  de.re.eeip.encapsulation.datatypes.CommonPacketFormat();
        commonPacketFormat.ItemCount = 0x02;

        commonPacketFormat.AddressItem = 0x0000;        //NULL (used for UCMM Messages)
        commonPacketFormat.AddressLength = 0x0000;

        commonPacketFormat.DataItem = 0xB2;
        commonPacketFormat.DataLength = 6;

        //----------------CIP Command "Get Attribute Single"
        commonPacketFormat.Data.add((byte) CIPCommonServicesEnum.Get_Attributes_All.getValue());
        //----------------CIP Command "Get Attribute Single"

        //----------------Requested Path size
        commonPacketFormat.Data.add((byte)2);
        //----------------Requested Path size

        //----------------Path segment for Class ID
        commonPacketFormat.Data.add((byte)0x20);
        commonPacketFormat.Data.add((byte)classID);
        //----------------Path segment for Class ID

        //----------------Path segment for Instance ID
        commonPacketFormat.Data.add((byte)0x24);
        commonPacketFormat.Data.add((byte)instanceID);
        //----------------Path segment for Instace ID
        byte[] dataToWrite = new byte[encapsulation.toBytes().length + commonPacketFormat.toBytes().length];
        System.arraycopy(encapsulation.toBytes(), 0, dataToWrite, 0, encapsulation.toBytes().length);
        System.arraycopy(commonPacketFormat.toBytes(), 0, dataToWrite, encapsulation.toBytes().length, commonPacketFormat.toBytes().length);

        outToServer.write(dataToWrite);

        byte[] data = new byte[564];
        int bytes = inFromServer.read(data);

        //--------------------------BEGIN Error?
        if (data[42] != 0)      //Exception codes see "Table B-1.1 CIP General Status Codes"
        {
            throw new de.re.eeip.cip.exception.CIPException(de.re.eeip.cip.datatypes.GeneralStatusCodes.GetStatusCode(data[42]));
        }
        //--------------------------END Error?

        byte[] returnData = new byte[bytes - 44];
        System.arraycopy(data, 44, returnData, 0, bytes-44);

        return returnData;
    }

    public byte[] SetAttributeSingle(int classID, int instanceID, int attributeID, byte[] value) throws CIPException, IOException
    {
        if (sessionHandle == 0)             //If a Session is not Registers, Try to Registers a Session with the predefined IP-Address and Port
            this.RegisterSession();
        byte[] dataToSend = new byte[48 + value.length];
        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.SessionHandle = sessionHandle;
        encapsulation.Command = CommandsEnum.SendRRData;
        encapsulation.Length = (24+value.length);
        //---------------Interface Handle CIP
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Interface Handle CIP

        //----------------Timeout
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Timeout

        //Common Packet Format (Table 2-6.1)
        de.re.eeip.encapsulation.datatypes.CommonPacketFormat commonPacketFormat = new  de.re.eeip.encapsulation.datatypes.CommonPacketFormat();
        commonPacketFormat.ItemCount = 0x02;

        commonPacketFormat.AddressItem = 0x0000;        //NULL (used for UCMM Messages)
        commonPacketFormat.AddressLength = 0x0000;

        commonPacketFormat.DataItem = 0xB2;
        commonPacketFormat.DataLength = (8 + value.length);



        //----------------CIP Command "Set Attribute Single"
        commonPacketFormat.Data.add((byte) CIPCommonServicesEnum.Set_Attribute_Single.getValue());
        //----------------CIP Command "Set Attribute Single"

        //----------------Requested Path size
        commonPacketFormat.Data.add((byte)3);
        //----------------Requested Path size

        //----------------Path segment for Class ID
        commonPacketFormat.Data.add((byte)0x20);
        commonPacketFormat.Data.add((byte)classID);
        //----------------Path segment for Class ID

        //----------------Path segment for Instance ID
        commonPacketFormat.Data.add((byte)0x24);
        commonPacketFormat.Data.add((byte)instanceID);
        //----------------Path segment for Instace ID

        //----------------Path segment for Attribute ID
        commonPacketFormat.Data.add((byte)0x30);
        commonPacketFormat.Data.add((byte)attributeID);
        //----------------Path segment for Attribute ID

        //----------------Data
        for (int i = 0; i < value.length; i++)
        {
            commonPacketFormat.Data.add(value[i]);
        }
        //----------------Data

        byte[] dataToWrite = new byte[encapsulation.toBytes().length + commonPacketFormat.toBytes().length];
        System.arraycopy(encapsulation.toBytes(), 0, dataToWrite, 0, encapsulation.toBytes().length);
        System.arraycopy(commonPacketFormat.toBytes(), 0, dataToWrite, encapsulation.toBytes().length, commonPacketFormat.toBytes().length);


        outToServer.write(dataToWrite);

        byte[] data = new byte[564];
        int bytes = inFromServer.read(data);

        //--------------------------BEGIN Error?
        if (data[42] != 0)      //Exception codes see "Table B-1.1 CIP General Status Codes"
        {
            throw new de.re.eeip.cip.exception.CIPException(de.re.eeip.cip.datatypes.GeneralStatusCodes.GetStatusCode(data[42]));
        }
        //--------------------------END Error?

        byte[] returnData = new byte[bytes - 44];
        System.arraycopy(data, 44, returnData, 0, bytes-44);
        return returnData;
    }

    public void ForwardOpen()   throws CIPException, IOException
    {
        int o_t_headerOffset = 2;                    //Zählt den Sequencecount und evtl 32bit header zu der Länge dazu
        if (o_t_realTimeFormat == RealTimeFormat.Header32Bit)
            o_t_headerOffset = 6;
        if (o_t_realTimeFormat == RealTimeFormat.Heartbeat)
            o_t_headerOffset = 0;

        int t_o_headerOffset = 2;                    //Zählt den Sequencecount und evtl 32bit header zu der Länge dazu
        if (t_o_realTimeFormat == RealTimeFormat.Header32Bit)
            t_o_headerOffset = 6;
        if (t_o_realTimeFormat == RealTimeFormat.Heartbeat)
            t_o_headerOffset = 0;

        int lengthOffset = (5 + (o_t_connectionType == ConnectionType.Null ? 0 : 2) + (t_o_connectionType == ConnectionType.Null ? 0 : 2));

        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.SessionHandle = sessionHandle;
        encapsulation.Command = CommandsEnum.SendRRData;
        //!!!!!!-----Length Field at the end!!!!!!!!!!!!!

        //---------------Interface Handle CIP
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Interface Handle CIP

        //----------------Timeout
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Timeout

        //Common Packet Format (Table 2-6.1)
        de.re.eeip.encapsulation.datatypes.CommonPacketFormat commonPacketFormat = new  de.re.eeip.encapsulation.datatypes.CommonPacketFormat();
        commonPacketFormat.ItemCount = 0x02;

        commonPacketFormat.AddressItem = 0x0000;        //NULL (used for UCMM Messages)
        commonPacketFormat.AddressLength = 0x0000;


        commonPacketFormat.DataItem = 0xB2;
        commonPacketFormat.DataLength = (41 + lengthOffset);



        //----------------CIP Command "Forward Open"
        commonPacketFormat.Data.add((byte)0x54);
        //----------------CIP Command "Forward Open"

        //----------------Requested Path size
        commonPacketFormat.Data.add((byte)2);
        //----------------Requested Path size

        //----------------Path segment for Class ID
        commonPacketFormat.Data.add((byte)0x20);
        commonPacketFormat.Data.add((byte)6);
        //----------------Path segment for Class ID

        //----------------Path segment for Instance ID
        commonPacketFormat.Data.add((byte)0x24);
        commonPacketFormat.Data.add((byte)1);
        //----------------Path segment for Instace ID

        //----------------Priority and Time/Tick - Table 3-5.16 (Vol. 1)
        commonPacketFormat.Data.add((byte)0x03);
        //----------------Priority and Time/Tick

        //----------------Timeout Ticks - Table 3-5.16 (Vol. 1)
        commonPacketFormat.Data.add((byte)0xfa);
        //----------------Timeout Ticks

        this.connectionID_O_T = new java.util.Random().nextInt(0xfffffff);
        this.connectionID_T_O = (new java.util.Random().nextInt(0xfffffff)+1);
        commonPacketFormat.Data.add((byte)connectionID_O_T);
        commonPacketFormat.Data.add((byte)(connectionID_O_T >> 8));
        commonPacketFormat.Data.add((byte)(connectionID_O_T >> 16));
        commonPacketFormat.Data.add((byte)(connectionID_O_T >> 24));


        commonPacketFormat.Data.add((byte)connectionID_T_O);
        commonPacketFormat.Data.add((byte)(connectionID_T_O >> 8));
        commonPacketFormat.Data.add((byte)(connectionID_T_O >> 16));
        commonPacketFormat.Data.add((byte)(connectionID_T_O >> 24));

        this.connectionSerialNumber = (new java.util.Random().nextInt(0xfffffff)+2);
        commonPacketFormat.Data.add((byte)connectionSerialNumber);
        commonPacketFormat.Data.add((byte)(connectionSerialNumber >> 8));

        //----------------Originator Vendor ID
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0);
        //----------------Originaator Vendor ID

        //----------------Originator Serial Number
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        //----------------Originator Serial Number

        //----------------Timeout Multiplier
        commonPacketFormat.Data.add((byte)3);
        //----------------Timeout Multiplier

        //----------------Reserved
        commonPacketFormat.Data.add((byte)0);
        commonPacketFormat.Data.add((byte)0);
        commonPacketFormat.Data.add((byte)0);
        //----------------Reserved

        //----------------Requested Packet Rate O->T in Microseconds
        commonPacketFormat.Data.add((byte) (requestedPacketRate_O_T));
        commonPacketFormat.Data.add((byte)((requestedPacketRate_O_T) >> 8));
        commonPacketFormat.Data.add((byte)((requestedPacketRate_O_T) >> 16));
        commonPacketFormat.Data.add((byte)((requestedPacketRate_O_T) >> 24));
        //----------------Requested Packet Rate O->T in Microseconds

        //----------------O->T Network Connection Parameters
        boolean redundantOwner = o_t_ownerRedundant;
        byte connectionType = o_t_connectionType.getValue(); //1=Multicast, 2=P2P
        byte priority = o_t_priority.getValue();         //00=low; 01=High; 10=Scheduled; 11=Urgent
        boolean variableLength = o_t_variableLength;       //0=fixed; 1=variable
        int connectionSize = (o_t_length + o_t_headerOffset);      //The maximum size in bytes og the data for each direction (were applicable) of the connection. For a variable -> maximum
        int NetworkConnectionParameters = (connectionSize & 0x1FF) | (((variableLength == true) ? 0x1 : 0x00) << 9) | ((priority & 0x03) << 10) | ((connectionType & 0x03) << 13) | (((redundantOwner == true) ? 0x1 : 0x00) << 15);
        commonPacketFormat.Data.add((byte)NetworkConnectionParameters);
        commonPacketFormat.Data.add((byte)(NetworkConnectionParameters >> 8));
        //----------------O->T Network Connection Parameters

        //----------------Requested Packet Rate T->O in Microseconds
        commonPacketFormat.Data.add((byte)requestedPacketRate_T_O);
        commonPacketFormat.Data.add((byte)(requestedPacketRate_T_O >> 8));
        commonPacketFormat.Data.add((byte)(requestedPacketRate_T_O >> 16));
        commonPacketFormat.Data.add((byte)(requestedPacketRate_T_O >> 24));
        //----------------Requested Packet Rate T->O in Microseconds

        //----------------T->O Network Connection Parameters
        redundantOwner = t_o_ownerRedundant;
        connectionType = t_o_connectionType.getValue(); //1=Multicast, 2=P2P
        priority = t_o_priority.getValue();         //00=low; 01=High; 10=Scheduled; 11=Urgent
        variableLength = t_o_variableLength;       //0=fixed; 1=variable
        connectionSize = (t_o_length + t_o_headerOffset);      //The maximum size in bytes og the data for each direction (were applicable) of the connection. For a variable -> maximum
        NetworkConnectionParameters = (connectionSize & 0x1FF) | (((variableLength == true) ? 0x1 : 0x00) << 9) | ((priority & 0x03) << 10) | ((connectionType & 0x03) << 13) | (((redundantOwner == true) ? 0x1 : 0x00) << 15);
        commonPacketFormat.Data.add((byte)NetworkConnectionParameters);
        commonPacketFormat.Data.add((byte)(NetworkConnectionParameters >> 8));
        //----------------T->O Network Connection Parameters

        //----------------Transport Type/Trigger
        commonPacketFormat.Data.add((byte)0x01);
        //X------- = 0= Client; 1= Server
        //-XXX---- = Production Trigger, 0 = Cyclic, 1 = CoS, 2 = Application Object
        //----XXXX = Transport class, 0 = Class 0, 1 = Class 1, 2 = Class 2, 3 = Class 3
        //----------------Transport Type Trigger
        //Connection Path size
        commonPacketFormat.Data.add((byte)((0x2) + (o_t_connectionType == ConnectionType.Null ? 0 : 1) + (t_o_connectionType == ConnectionType.Null ? 0 : 1) ));
        //Verbindugspfad
        commonPacketFormat.Data.add((byte)(0x20));
        commonPacketFormat.Data.add((byte)(assemblyObjectClass));
        commonPacketFormat.Data.add((byte)(0x24));
        commonPacketFormat.Data.add((byte)(0x01));
        if (o_t_connectionType != ConnectionType.Null)
        {
            commonPacketFormat.Data.add((byte)(0x2C));
            commonPacketFormat.Data.add((byte)(o_t_instanceID));
        }
        if (t_o_connectionType != ConnectionType.Null)
        {
            commonPacketFormat.Data.add((byte)(0x2C));
            commonPacketFormat.Data.add((byte)(t_o_instanceID));
        }

        //AddSocket Addrress Item O->T

        commonPacketFormat.SocketaddrInfo_O_T = new de.re.eeip.encapsulation.datatypes.SocketAddress();
        commonPacketFormat.SocketaddrInfo_O_T.SIN_port = originatorUDPPort;
        commonPacketFormat.SocketaddrInfo_O_T.SIN_family = 2;
        if (o_t_connectionType == ConnectionType.Multicast)
        {
            String[] addressSubstring = ipAddress.split("\\.");
            int addressByte0 = Integer.parseInt(addressSubstring[0]);
            int addressByte1 = Integer.parseInt(addressSubstring[1]);
            int addressByte2 = Integer.parseInt(addressSubstring[2]);
            int addressByte3 = Integer.parseInt(addressSubstring[3]);

            int ipAddressInt = (0xff & addressByte3) + (((0xff & addressByte2)) << 8) + ((0xff & addressByte1) << 16) + ((0xff & addressByte0) << 24);


            int multicastResponseAddress = EEIPClient.GetMulticastAddress(ipAddressInt);

            commonPacketFormat.SocketaddrInfo_O_T.SIN_Address = (multicastResponseAddress);

            multicastAddress = commonPacketFormat.SocketaddrInfo_O_T.SIN_Address;
        }
        else
            commonPacketFormat.SocketaddrInfo_O_T.SIN_Address = 0;

        encapsulation.Length = (commonPacketFormat.toBytes().length+6);//(ushort)(57 + (ushort)lengthOffset);
        //20 04 24 01 2C 65 2C 6B

        byte[] dataToWrite = new byte[encapsulation.toBytes().length + commonPacketFormat.toBytes().length];
        System.arraycopy(encapsulation.toBytes(), 0, dataToWrite, 0, encapsulation.toBytes().length);
        System.arraycopy(commonPacketFormat.toBytes(), 0, dataToWrite, encapsulation.toBytes().length, commonPacketFormat.toBytes().length);
       //encapsulation.toBytes();


        outToServer.write(dataToWrite);

        byte[] data = new byte[564];
        int bytes = inFromServer.read(data);


        //--------------------------BEGIN Error?
        if (data[42] != 0)      //Exception codes see "Table B-1.1 CIP General Status Codes"
        {
            if (data[42] == 0x1)
                if (data[43] == 0)
                    throw new CIPException("Connection failure, General Status Code: " + data[42]);
                else
                    throw new CIPException("Connection failure, General Status Code: " + data[42] + " Additional Status Code: " + ((data[45] << 8) | data[44]) + " " + de.re.eeip.objectlibrary.ConnectionManagerObject.GetExtendedStatus((((0xff & data[45]) << 8) | (0xff & data[44]))));
            else
                throw new CIPException(de.re.eeip.cip.datatypes.GeneralStatusCodes.GetStatusCode(data[42]));
        }
        //--------------------------END Error?
        //Read the Network ID from the Reply (see 3-3.7.1.1)
        int itemCount = data[30] + (data[31] << 8);
        int lengthUnconectedDataItem = data[38] + (data[39] << 8);
        //System.out.println("created Connection ID O->T: " +connectionID_O_T);
        //System.out.println("created Connection ID T->O: " +connectionID_T_O);
        this.connectionID_O_T = (data[44] & 0xff) + ((0xff & data[45]) << 8) + ((0xff & data[46]) << 16) + ((0xff & data[47]) << 24);
        this.connectionID_T_O = (data[48] & 0xff) + ((0xff & data[49]) << 8) + ((0xff & data[50]) << 16) + ((0xff & data[51]) << 24);
        //System.out.println("Received Connection ID O->T: " +connectionID_O_T);
        //System.out.println("Received Connection ID T->O: " +connectionID_T_O);
        //Is a SocketInfoItem present?
        int numberOfCurrentItem = 0;
        de.re.eeip.encapsulation.datatypes.SocketAddress socketInfoItem;
        while (itemCount > 2)
        {
            int typeID = data[40 + lengthUnconectedDataItem+ 20 * numberOfCurrentItem] + ((0xff & data[40 + lengthUnconectedDataItem + 1+ 20 * numberOfCurrentItem]) << 8);
            if (typeID == 0x8001)
            {
                socketInfoItem = new de.re.eeip.encapsulation.datatypes.SocketAddress();
                socketInfoItem.SIN_Address = (0xff & data[40 + lengthUnconectedDataItem + 11 + 20 * numberOfCurrentItem]) + ((0xff & data[40 + lengthUnconectedDataItem + 10 + 20 * numberOfCurrentItem]) << 8) + ((0xff & data[40 + lengthUnconectedDataItem + 9 + 20 * numberOfCurrentItem]) << 16) + ((0xff & data[40 + lengthUnconectedDataItem + 8 + 20 * numberOfCurrentItem]) << 24);
                socketInfoItem.SIN_port = ((0xff & data[40 + lengthUnconectedDataItem + 7 + 20 * numberOfCurrentItem]) + ((0xff & data[40 + lengthUnconectedDataItem + 6 + 20 * numberOfCurrentItem]) << 8));
                if (t_o_connectionType == ConnectionType.Multicast)
                    multicastAddress = socketInfoItem.SIN_Address;
                targetUDPPort = (int)socketInfoItem.SIN_port;
            }
            numberOfCurrentItem++;
            itemCount--;
        }
        //Open UDP-Port
        if (multicastAddress != 0)
            listenerThread = new UdpListenerThread(de.re.eeip.encapsulation.CipIdentityItem.getIPAddress(multicastAddress));
        else
            listenerThread = new UdpListenerThread(null);

        listenerThread.start();

        sendThread = new UdpSendThread() ;
        sendThread.start();
    }

    public void ForwardClose() throws CIPException, IOException
    {
        //First stop the Thread which send data
        sendThread.interrupt();


        int lengthOffset = (5 + (o_t_connectionType == ConnectionType.Null ? 0 : 2) + (t_o_connectionType == ConnectionType.Null ? 0 : 2));

        de.re.eeip.encapsulation.Encapsulation encapsulation = new de.re.eeip.encapsulation.Encapsulation();
        encapsulation.SessionHandle = sessionHandle;
        encapsulation.Command = CommandsEnum.SendRRData;
        encapsulation.Length = (16+17+lengthOffset);

        //---------------Interface Handle CIP
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Interface Handle CIP

        //----------------Timeout
        encapsulation.CommandSpecificData.add((byte)0);
        encapsulation.CommandSpecificData.add((byte)0);
        //----------------Timeout

        //Common Packet Format (Table 2-6.1)
        de.re.eeip.encapsulation.datatypes.CommonPacketFormat commonPacketFormat = new  de.re.eeip.encapsulation.datatypes.CommonPacketFormat();
        commonPacketFormat.ItemCount = 0x02;

        commonPacketFormat.AddressItem = 0x0000;        //NULL (used for UCMM Messages)
        commonPacketFormat.AddressLength = 0x0000;


        commonPacketFormat.DataItem = 0xB2;
        commonPacketFormat.DataLength = (17 + lengthOffset);



        //----------------CIP Command "Forward Close"
        commonPacketFormat.Data.add((byte)0x4E);
        //----------------CIP Command "Forward Close"

        //----------------Requested Path size
        commonPacketFormat.Data.add((byte)2);
        //----------------Requested Path size

        //----------------Path segment for Class ID
        commonPacketFormat.Data.add((byte)0x20);
        commonPacketFormat.Data.add((byte)6);
        //----------------Path segment for Class ID

        //----------------Path segment for Instance ID
        commonPacketFormat.Data.add((byte)0x24);
        commonPacketFormat.Data.add((byte)1);
        //----------------Path segment for Instace ID

        //----------------Priority and Time/Tick - Table 3-5.16 (Vol. 1)
        commonPacketFormat.Data.add((byte)0x03);
        //----------------Priority and Time/Tick

        //----------------Timeout Ticks - Table 3-5.16 (Vol. 1)
        commonPacketFormat.Data.add((byte)0xfa);
        //----------------Timeout Ticks

        //Connection serial number
        commonPacketFormat.Data.add((byte)connectionSerialNumber);
        commonPacketFormat.Data.add((byte)(connectionSerialNumber >> 8));
        //connection seruial number

        //----------------Originator Vendor ID
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0);
        //----------------Originaator Vendor ID

        //----------------Originator Serial Number
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        commonPacketFormat.Data.add((byte)0xFF);
        //----------------Originator Serial Number

        //Connection Path size
        commonPacketFormat.Data.add((byte)((0x2) + (o_t_connectionType == ConnectionType.Null ? 0 : 1) + (t_o_connectionType == ConnectionType.Null ? 0 : 1)));
        //Reserved
        commonPacketFormat.Data.add((byte)0);
        //Reserved


        //Verbindugspfad
        commonPacketFormat.Data.add((byte)(0x20));
        commonPacketFormat.Data.add((byte)(0x4));
        commonPacketFormat.Data.add((byte)(0x24));
        commonPacketFormat.Data.add((byte)(0x01));
        if (o_t_connectionType != ConnectionType.Null)
        {
            commonPacketFormat.Data.add((byte)(0x2C));
            commonPacketFormat.Data.add((byte)(o_t_instanceID));
        }
        if (t_o_connectionType != ConnectionType.Null)
        {
            commonPacketFormat.Data.add((byte)(0x2C));
            commonPacketFormat.Data.add((byte)(t_o_instanceID));
        }

        byte[] dataToWrite = new byte[encapsulation.toBytes().length + commonPacketFormat.toBytes().length];
        System.arraycopy(encapsulation.toBytes(), 0, dataToWrite, 0, encapsulation.toBytes().length);
        System.arraycopy(commonPacketFormat.toBytes(), 0, dataToWrite, encapsulation.toBytes().length, commonPacketFormat.toBytes().length);


        outToServer.write(dataToWrite);

        byte[] data = new byte[564];
        int bytes = inFromServer.read(data);


        //--------------------------BEGIN Error?
        if (data[42] != 0)      //Exception codes see "Table B-1.1 CIP General Status Codes"
        {
            throw new CIPException("Connection failure, General Status Code: " + data[42]);
        }


        //Close the Socket for Receive
        listenerThread.interrupt();
        sendThread.interrupt();




    }


    /**
     * Implementation of Common Service "Get_Attribute_All" - Service Code: 0x01
     * @param classID   Class id of requested Attributes
     * @return
     */
    public byte[] GetAttributeAll(int classID) throws IOException, CIPException {
        return this.GetAttributeAll(classID, 0);
    }

    /**
     * Converts a bytearray (received e.g. via getAttributeSingle) to ushort (int in java)
     * @param byteArray bytearray to convert
     * @return
     */
    public static int ToUshort(byte[] byteArray)
    {
        int returnValue = 0;
        returnValue = ((0xff & byteArray[1]) << 8 | (0xff & byteArray[0]));
        return returnValue;
    }

    /**
     * Converts a bytearray (received e.g. via getAttributeSingle) to uint (ling in java)
     * @param byteArray bytearray to convert
     * @return
     */
    public static long ToUint(byte[] byteArray)
    {
        long returnValue = 0;
        returnValue = ((0xff & byteArray[3]) << 24 | (0xff & byteArray[2]) << 16 | (0xff & byteArray[1]) << 8 | (0xff & byteArray[0]));
        return returnValue;
    }

    /**
     * Returns the "Bool" State of a byte Received via getAttributeSingle
     * @param inputByte     byte to convert
     * @param bitposition   bitposition to convert (First bit = bitposition 0)
     * @return              Converted bool value
     */
    public static boolean ToBool(byte inputByte, int bitposition)
    {
        return (((inputByte>>bitposition)&0x01) != 0) ? true : false;
    }

    private static int GetMulticastAddress(int deviceIPAddress)
    {
        int cip_Mcast_Base_Addr = 0xEFC00100;
        int cip_Host_Mask = 0x3FF;
        int netmask = 0;

        //Class A Network?
        if (deviceIPAddress <= 0x7FFFFFFF)
            netmask = 0xFF000000;
        //Class B Network?
        if (deviceIPAddress >= 0x80000000 && deviceIPAddress <= 0xBFFFFFFF)
            netmask = 0xFFFF0000;
        //Class C Network?
        if (deviceIPAddress >= 0xC0000000 && deviceIPAddress <= 0xDFFFFFFF)
            netmask = 0xFFFFFF00;

        int hostID = deviceIPAddress & ~netmask;
        int mcastIndex = hostID - 1;
        mcastIndex = mcastIndex & cip_Host_Mask;

        return (cip_Mcast_Base_Addr + mcastIndex * (int)32);
    }

    private class UdpListenerThread extends Thread
    {

        String multicastAddress;

        byte[] buf = new byte[1024];
        MulticastSocket socket;


        UdpListenerThread(String multicastAddress)
        {
            this.multicastAddress = multicastAddress;
        }

        public void run()
        {

            try {
                if (multicastAddress == null)
                    socket = new MulticastSocket(originatorUDPPort);
                else
                {
                    InetAddress group = InetAddress.getByName(multicastAddress);
                    socket = new MulticastSocket(originatorUDPPort);
                    socket.joinGroup(group);
                }
                socket.setSoTimeout((int)(10*requestedPacketRate_T_O/1000));
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!this.isInterrupted())
            {
                try {
                DatagramPacket packet
                        = new DatagramPacket(buf, buf.length);

                    socket.receive(packet);
                int numberOfBytes = packet.getLength();
                byte[] receivedBytes = packet.getData();
                if (numberOfBytes > 20)
                {
                    //Get the connection ID
                    int connectionID = ((0xff & receivedBytes[6]) | (0xff & receivedBytes[7]) << 8 | (0xff & receivedBytes[8]) << 16 | (0xff & receivedBytes[9]) << 24);


                    if (connectionID == connectionID_T_O)
                    {
                        int headerOffset = 0;
                        if (t_o_realTimeFormat == RealTimeFormat.Header32Bit)
                            headerOffset = 4;
                        if (t_o_realTimeFormat == RealTimeFormat.Heartbeat)
                            headerOffset = 0;
                        for (int i = 0; i < numberOfBytes-20-headerOffset; i++)
                        {
                            T_O_IOData[i] = receivedBytes[20 + i + headerOffset];
                        }
                        //Console.WriteLine(T_O_IOData[0]);


                    }
                }
                //LastReceivedImplicitMessage = DateTime.Now;
                } catch (Exception e) {
                    if (!this.isInterrupted())
                    e.printStackTrace();
                }
            }
            socket.disconnect();
            socket.close();

        }
    }

    private class UdpSendThread extends Thread {
        DatagramSocket socket;
        int sequenceCount = 0;
        int sequence = 0;
        public void run()
        {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            try {
            while (!isInterrupted())
            {

                byte[] o_t_IOData = new byte[564];

                //---------------Item count
                o_t_IOData[0] = 2;
                o_t_IOData[1] = 0;
                //---------------Item count

                //---------------Type ID
                o_t_IOData[2] = 0x02;
                o_t_IOData[3] = (byte)(0xff & 0x80);
                //---------------Type ID

                //---------------Length
                o_t_IOData[4] = 0x08;
                o_t_IOData[5] = 0x00;
                //---------------Length

                //---------------connection ID
                sequenceCount++;
                o_t_IOData[6] = (byte)(connectionID_O_T);
                o_t_IOData[7] = (byte)(connectionID_O_T >> 8);
                o_t_IOData[8] = (byte)(connectionID_O_T >> 16);
                o_t_IOData[9] = (byte)(connectionID_O_T >> 24);
                //---------------connection ID

                //---------------sequence count
                o_t_IOData[10] = (byte)(sequenceCount);
                o_t_IOData[11] = (byte)(sequenceCount >> 8);
                o_t_IOData[12] = (byte)(sequenceCount >> 16);
                o_t_IOData[13] = (byte)(sequenceCount >> 24);
                //---------------sequence count

                //---------------Type ID
                o_t_IOData[14] = (byte)(0xff & 0xB1);
                o_t_IOData[15] = 0x00;
                //---------------Type ID

                int headerOffset = 0;
                if (o_t_realTimeFormat == RealTimeFormat.Header32Bit)
                    headerOffset = 4;
                if (o_t_realTimeFormat == RealTimeFormat.Heartbeat)
                    headerOffset = 0;
                int o_t_Length = (o_t_length + headerOffset+2);   //Modeless and zero Length

                //---------------Length
                o_t_IOData[16] = (byte)o_t_Length;
                o_t_IOData[17] = (byte)(o_t_Length >> 8);
                //---------------Length

                //---------------Sequence count
                sequence++;
                if (o_t_realTimeFormat != RealTimeFormat.Heartbeat)
                {
                    o_t_IOData[18] = (byte)sequence;
                    o_t_IOData[19] = (byte)(sequence >> 8);
                }
                //---------------Sequence count

                if (o_t_realTimeFormat == RealTimeFormat.Header32Bit)
                {
                    o_t_IOData[20] = (byte)1;
                    o_t_IOData[21] = (byte)0;
                    o_t_IOData[22] = (byte)0;
                    o_t_IOData[23] = (byte)0;

                }

                //---------------Write data
                for ( int i = 0; i < o_t_length; i++)
                    o_t_IOData[20+headerOffset+i] = (byte)O_T_IOData[i];
                //---------------Write data


                DatagramPacket packet
                        = null;
                try {
                    packet = new DatagramPacket(o_t_IOData, 20+headerOffset+o_t_length, InetAddress.getByName( ipAddress ), targetUDPPort);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }


                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                    Thread.sleep(requestedPacketRate_O_T/1000);


            }
            } catch (InterruptedException e) {
            }
            socket.disconnect();
            socket.close();
        }
    }


    public void setO_T_InstanceID(int instanceID)
    {
        this.o_t_instanceID = instanceID;
    }

    public void setT_O_InstanceID(int instanceID)
    {
        this.t_o_instanceID = instanceID;
    }

    public void setO_T_Length(int length)
    {
        this.o_t_length = length;
    }

    public void setT_O_Length(int length)
    {
        this.t_o_length = length;
    }

    public void setO_T_RealTimeFormat(de.re.eeip.cip.datatypes.RealTimeFormat realTimeFormat)
    {
        this.o_t_realTimeFormat = realTimeFormat;
    }

    public void setT_O_RealTimeFormat(de.re.eeip.cip.datatypes.RealTimeFormat realTimeFormat)
    {
        this.t_o_realTimeFormat = realTimeFormat;
    }

    public void setO_T_ownerRedundant(boolean ownerRedundant)
    {
        this.o_t_ownerRedundant = ownerRedundant;
    }

    public void setT_O_ownerRedundant(boolean ownerRedundant)
    {
        this.t_o_ownerRedundant = ownerRedundant;
    }

    public void setO_T_priority(de.re.eeip.cip.datatypes.Priority priority)
    {
        this.o_t_priority = priority;
    }

    public void setT_O_priority(de.re.eeip.cip.datatypes.Priority priority)
    {
        this.t_o_priority = priority;
    }

    public void setO_T_variableLength(boolean variableLength)
    {
        this.o_t_variableLength = variableLength;
    }

    public void setT_O_variableLength(boolean variableLength)
    {
        this.t_o_variableLength = variableLength;
    }

    public void setO_T_connectionType(de.re.eeip.cip.datatypes.ConnectionType connectionType)
    {
        this.o_t_connectionType = connectionType;
    }

    public void setT_O_connectionType(de.re.eeip.cip.datatypes.ConnectionType connectionType)
    {
        this.t_o_connectionType = connectionType;
    }

    public void setTcpPort(int tcpPort)
    {
        this.tcpPort = tcpPort;
    }

    public void setOriginatorUDPPort(int originatorUDPPort)
    {
        this.originatorUDPPort = originatorUDPPort;
    }

    public void setTargetUDPPort(int targetUDPPort)
    {
        this.targetUDPPort = targetUDPPort;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public void setRequestedPacketRate_O_T(long requestedPacketRate_O_T)
    {
        this.requestedPacketRate_O_T = requestedPacketRate_O_T;
    }

    public void setRequestedPacketRate_T_O(long requestedPacketRate_T_O)
    {
        this.requestedPacketRate_T_O = requestedPacketRate_T_O;
    }
}
