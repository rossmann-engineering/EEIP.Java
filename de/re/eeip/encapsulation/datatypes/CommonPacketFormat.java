package de.re.eeip.encapsulation.datatypes;

import java.util.ArrayList;

/**
 * Created by sr555 on 26.09.17.
 */
public class CommonPacketFormat {

        public int ItemCount = 2;
        public int AddressItem = 0x0000;
        public int AddressLength = 0;
        public int DataItem = 0xB2; //0xB2 = Unconnected Data Item
        public int DataLength = 8;
        public ArrayList Data = new ArrayList();
        public int SockaddrInfoItem_O_T = 0x8001; //8000 for O->T and 8001 for T->O - Volume 2 Table 2-6.9
        public int SockaddrInfoLength = 16;
        public SocketAddress SocketaddrInfo_O_T = null;


        public byte[] toBytes()
        {
            if (SocketaddrInfo_O_T != null)
                ItemCount=3;
            byte[] returnValue = new byte[10 + Data.size() + (SocketaddrInfo_O_T == null ? 0 : 20)];
            returnValue[0] = (byte)this.ItemCount;
            returnValue[1] = (byte)(this.ItemCount >> 8);
            returnValue[2] = (byte)this.AddressItem;
            returnValue[3] = (byte)(this.AddressItem >> 8);
            returnValue[4] = (byte)this.AddressLength;
            returnValue[5] = (byte)(this.AddressLength >> 8);
            returnValue[6] = (byte)this.DataItem;
            returnValue[7] = (byte)(this.DataItem >> 8);
            returnValue[8] = (byte)this.DataLength;
            returnValue[9] = (byte)(this.DataLength >> 8);
            for (int i = 0; i < Data.size(); i++)
            {
                returnValue[10 + i] = (byte) Data.get(i);
            }


            // Add Socket Address Info Item
            if (SocketaddrInfo_O_T != null)
            {
                returnValue[10 + Data.size() + 0] = (byte)this.SockaddrInfoItem_O_T;
                returnValue[10 + Data.size() + 1] = (byte)(this.SockaddrInfoItem_O_T >> 8);
                returnValue[10 + Data.size() + 2] = (byte)this.SockaddrInfoLength;
                returnValue[10 + Data.size() + 3] = (byte)(this.SockaddrInfoLength >> 8);
                returnValue[10 + Data.size() + 5] = (byte)this.SocketaddrInfo_O_T.SIN_family;
                returnValue[10 + Data.size() + 4] = (byte)(this.SocketaddrInfo_O_T.SIN_family >> 8);
                returnValue[10 + Data.size() + 7] = (byte)this.SocketaddrInfo_O_T.SIN_port;
                returnValue[10 + Data.size() + 6] = (byte)(this.SocketaddrInfo_O_T.SIN_port >> 8);
                returnValue[10 + Data.size() + 11] = (byte)this.SocketaddrInfo_O_T.SIN_Address;
                returnValue[10 + Data.size() + 10] = (byte)(this.SocketaddrInfo_O_T.SIN_Address >> 8);
                returnValue[10 + Data.size() + 9] = (byte)(this.SocketaddrInfo_O_T.SIN_Address >> 16);
                returnValue[10 + Data.size() + 8] = (byte)(this.SocketaddrInfo_O_T.SIN_Address >> 24);
                returnValue[10 + Data.size() + 12] = this.SocketaddrInfo_O_T.SIN_Zero[0];
                returnValue[10 + Data.size() + 13] = this.SocketaddrInfo_O_T.SIN_Zero[1];
                returnValue[10 + Data.size() + 14] = this.SocketaddrInfo_O_T.SIN_Zero[2];
                returnValue[10 + Data.size() + 15] = this.SocketaddrInfo_O_T.SIN_Zero[3];
                returnValue[10 + Data.size() + 16] = this.SocketaddrInfo_O_T.SIN_Zero[4];
                returnValue[10 + Data.size() + 17] = this.SocketaddrInfo_O_T.SIN_Zero[5];
                returnValue[10 + Data.size() + 18] = this.SocketaddrInfo_O_T.SIN_Zero[6];
                returnValue[10 + Data.size() + 19] = this.SocketaddrInfo_O_T.SIN_Zero[7];
            }
            return returnValue;

    }
}
