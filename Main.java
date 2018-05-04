import de.re.eeip.EEIPClient;
import de.re.eeip.cip.datatypes.ConnectionType;
import de.re.eeip.cip.datatypes.Priority;
import de.re.eeip.cip.datatypes.RealTimeFormat;
import de.re.eeip.cip.exception.CIPException;

import java.io.IOException;

/**
 * This example shows the Implicit Messaging of a Wago 750-352 in Java
 * Hardware Configuration:
 * Coupler:         750-352
 * Digital Input:   750-402 4-Channel
 * Digital Input:   Beckhoff KL1012 2-Channel
 * Analog Input:    Beckhoff KL3202 2-Channel
 * Digital Outout:  750-501 2-Channel
 * Termination:     750-600
 *
 * Documentation: http://www.wago.de/download.esm?file=%5Cdownload%5C00368362_0.pdf&name=m07500352_xxxxxxxx_0en.pdf
 */
public class Main
{
    public static void main(String[] args) throws InterruptedException {
        EEIPClient eipClient = new EEIPClient();

        try {
            System.out.println("Fisrst Register a session");
            eipClient.RegisterSession("192.168.1.3");

            //Parameters for Originator -> Target communication
            eipClient.setO_T_InstanceID(101);       //Output Assembly 65hex
            eipClient.setO_T_Length(1);
            eipClient.setO_T_RealTimeFormat(RealTimeFormat.Header32Bit);
            eipClient.setO_T_ownerRedundant(false);
            eipClient.setO_T_priority(Priority.Urgent);
            eipClient.setO_T_variableLength(false);
            eipClient.setO_T_connectionType(ConnectionType.Point_to_Point);

            //Parameters for Target -> Originator communication
            eipClient.setT_O_InstanceID(104);       //Input Assembly 68hex
            eipClient.setT_O_Length(6);
            eipClient.setT_O_RealTimeFormat(RealTimeFormat.Modeless);
            eipClient.setT_O_ownerRedundant(false);
            eipClient.setT_O_priority(Priority.Urgent);
            eipClient.setT_O_variableLength(false);
            eipClient.setT_O_connectionType(ConnectionType.Multicast);
            eipClient.setO_T_IOData(new byte[] {(byte)0xff});

            System.out.println("Send Forward open to initiate IO Messaging");
            eipClient.ForwardOpen();
            for (int i = 0; i < 50; i++)
            {
                byte[] T_O_IOData = eipClient.getT_O_IOData(6);
                System.out.println("Byte 0: "+T_O_IOData[0]);
                System.out.println("Byte 1: "+T_O_IOData[1]);
                System.out.println("Byte 2: "+T_O_IOData[2]);
                System.out.println("Byte 3: "+T_O_IOData[3]);
                System.out.println("Byte 4: "+T_O_IOData[4]);
                System.out.println("Byte 5: "+T_O_IOData[5]);
                Thread.sleep(500);
            }
            System.out.println("Send Forward Close");
            eipClient.ForwardClose();


            System.out.println("Unregister Session");
            eipClient.UnRegisterSession();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CIPException e) {
            e.printStackTrace();
        }

    }
}
