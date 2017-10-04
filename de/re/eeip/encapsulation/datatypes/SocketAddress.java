package de.re.eeip.encapsulation.datatypes;

/**
 *  Socket Address (see section 2-6.3.2)
 */
public class SocketAddress {
        public int SIN_family = 0;
        public long SIN_port = 0;
        public long SIN_Address = 0;
        public byte[] SIN_Zero = new byte[8];

        public SocketAddress()
        {
                SIN_family = 0;
                SIN_port = 0;
                SIN_Address = 0;
        }

}
