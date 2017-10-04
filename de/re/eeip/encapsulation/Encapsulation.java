package de.re.eeip.encapsulation;

import de.re.eeip.encapsulation.datatypes.StatusEnum;

import java.util.ArrayList;

/**
 * Created by sr555 on 26.09.17.
 */
public class Encapsulation {
        public de.re.eeip.encapsulation.datatypes.CommandsEnum Command;
        public int Length;
        public int SessionHandle;
        public de.re.eeip.encapsulation.datatypes.StatusEnum Status = StatusEnum.Success;
        private byte[] senderContext = new byte[8];
        private int options = 0;
        public ArrayList CommandSpecificData = new ArrayList();



        public byte[] toBytes()
        {
            byte[] returnValue = new byte[24 + CommandSpecificData.size()];
            returnValue[0] = (byte)this.Command.getValue();
            returnValue[1] = (byte)(this.Command.getValue() >> 8);
            returnValue[2] = (byte)this.Length;
            returnValue[3] = (byte)(this.Length >> 8);
            returnValue[4] = (byte)this.SessionHandle;
            returnValue[5] = (byte)(this.SessionHandle >> 8);
            returnValue[6] = (byte)(this.SessionHandle >> 16);
            returnValue[7] = (byte)(this.SessionHandle >> 24);
            returnValue[8] = (byte)this.Status.getValue();
            returnValue[9] = (byte)(this.Status.getValue() >> 8);
            returnValue[10] = (byte)(this.Status.getValue() >> 16);
            returnValue[11] = (byte)(this.Status.getValue() >> 24);
            returnValue[12] = senderContext[0];
            returnValue[13] = senderContext[1];
            returnValue[14] = senderContext[2];
            returnValue[15] = senderContext[3];
            returnValue[16] = senderContext[4];
            returnValue[17] = senderContext[5];
            returnValue[18] = senderContext[6];
            returnValue[19] = senderContext[7];
            returnValue[20] = (byte)this.options;
            returnValue[21] = (byte)(this.options >> 8);
            returnValue[22] = (byte)(this.options >> 16);
            returnValue[23] = (byte)(this.options >> 24);
            for (int i = 0; i < CommandSpecificData.size(); i++)
            {
                returnValue[24 + i] = (byte) CommandSpecificData.get(i);
            }
            return returnValue;
        }




    }

