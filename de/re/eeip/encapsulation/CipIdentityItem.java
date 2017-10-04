package de.re.eeip.encapsulation;

/**
 * Table 2-4.4 CIP Identity Item
 */
public class CipIdentityItem {


        public int ItemTypeCode;                                     //Code indicating item type of CIP Identity (0x0C)
        public int ItemLength;                                       //Number of bytes in item which follow (length varies depending on Product Name string)
        public int EncapsulationProtocolVersion;                     //Encapsulation Protocol Version supported (also returned with Register Sesstion reply).
        public de.re.eeip.encapsulation.datatypes.SocketAddress SocketAddress = new de.re.eeip.encapsulation.datatypes.SocketAddress();       //Socket Address (see section 2-6.3.2)
        public int VendorID1 = 0;                                        //Device manufacturers Vendor ID
        public int DeviceType1 = 0;                                      //Device Type of product
        public int ProductCode1 = 0;                                     //Product Code assigned with respect to device type
        public byte[] Revision1 = new byte[2];                       //Device revision
        public int Status1 = 0;                                          //Current status of device
        public long SerialNumber1 = 0;                               //Serial number of device
        public byte ProductNameLength;
        public String ProductName1;                                     //Human readable description of device
        public byte State1;                                             //Current state of device
        private long sin_port = 0;

        public CipIdentityItem getCIPIdentityItem(int startingByte, byte[] receivedData)
        {
            startingByte = startingByte + 2;            //Skipped ItemCount
            this.ItemTypeCode = ((0xff & receivedData[0+startingByte])
                    | ((0xff & receivedData[1 + startingByte]) << 8));
            this.ItemLength = ((0xff & receivedData[2 + startingByte])
                    | ((0xff & receivedData[3 + startingByte]) << 8));
            this.EncapsulationProtocolVersion = ((0xff & receivedData[4 + startingByte])
                    | ((0xff & receivedData[5 + startingByte]) << 8));
            this.SocketAddress.SIN_family = ((0xff & receivedData[7 + startingByte])
                    | ((0xff & receivedData[6 + startingByte]) << 8));
            this.SocketAddress.SIN_port = ((0xff & receivedData[9 + startingByte])
                    | ((receivedData[8 + startingByte] & 0xff) << 8));
            this.SocketAddress.SIN_Address = ((0xff & receivedData[13 + startingByte])
                    | ((0xff & receivedData[12 + startingByte]) << 8)
                    | ((0xff & receivedData[11 + startingByte]) << 16)
                    | ((0xff & receivedData[10 + startingByte]) << 24));
            this.VendorID1 = ((0xff & receivedData[22 + startingByte])
                    | ((0xff & receivedData[23 + startingByte]) << 8));
            this.DeviceType1 = ((0xff & receivedData[24 + startingByte])
                    | ((0xff & receivedData[25 + startingByte]) << 8));
            this.ProductCode1 = ((0xff & receivedData[26 + startingByte])
                    | ((0xff & receivedData[27 + startingByte]) << 8));
            this.Revision1[0] = receivedData[28 + startingByte];
            this.Revision1[1] = receivedData[29 + startingByte];
            this.Status1 = ((0xff & receivedData[30 + startingByte])
                    | ((0xff & receivedData[31 + startingByte]) << 8));
            this.SerialNumber1 = ((long)(0xff & receivedData[32 + startingByte])
                    | ((long)(0xff & receivedData[33 + startingByte]) << 8)
                    | ((long)(0xff & receivedData[34 + startingByte]) << 16)
                    | ((long)(0xff & receivedData[35 + startingByte]) << 24)
                    | ((long)(0xff & 0) << 32)
                    | ((long)(0xff & 0) << 40)
                    | ((long)(0xff & 0) << 48)
                    | ((long)(0xff & 0) << 56));
            this.ProductNameLength = receivedData[36 + startingByte];

            byte[] substring = new byte[this.ProductNameLength];
            System.arraycopy(receivedData, 37 + startingByte,substring,0,substring.length);

            this.ProductName1 = new String(substring);
            this.State1 = receivedData[receivedData.length - 1];
            return this;

    }
    /// <summary>
    /// Converts an IP-Address in UIint32 Format (Received by Device)
    /// </summary>
    public static String getIPAddress(long address)
    {
        return String.valueOf((int)0xff & ((address) >> 24))+"." + String.valueOf((int)(0xff & (address >> 16)))+"."+String.valueOf((int)(0xff & (address >> 8)))+"."+String.valueOf((int)(0xff & (address)));
    }
}
