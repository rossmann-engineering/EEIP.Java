import de.re.eeip.EEIPClient;
import de.re.eeip.cip.datatypes.ConnectionType;
import de.re.eeip.cip.datatypes.Priority;
import de.re.eeip.cip.datatypes.RealTimeFormat;
import de.re.eeip.cip.exception.CIPException;

import java.io.IOException;

/**
 * This example shows the Implicit Messaging of a Allen Bradley 1734-AENT Point I/O in Java
 * Hardware Configuration:
 * Coupler:         1734-AENT
 * Digital Input:   1734-IB4 4-Channel
 * Digital Input:   1734-IB4 4-Channel
 * Digital Input:   1734-IB4 4-Channel
 * Digital Input:   1734-IB4 4-Channel
 * Digital Output:  1734-OB4E 4-Channel
 * Digital Output:  1734-OB4E 4-Channel
 * Digital Output:  1734-OB4E 4-Channel
 * Digital Output:  1734-OB4E 4-Channel
 */
public class MainAllenBradley
{


    public static void main(String[] args) throws InterruptedException {
        EEIPClient eipClient = new EEIPClient();

        try {
            System.out.println("Fisrst Register a session");
            eipClient.RegisterSession("192.168.1.3");

            //Parameters for Originator -> Target communication
            eipClient.setO_T_InstanceID(100);       //Output Assembly 64hex
            eipClient.setO_T_Length(4);
            eipClient.setO_T_RealTimeFormat(RealTimeFormat.Header32Bit);
            eipClient.setO_T_ownerRedundant(false);
            eipClient.setO_T_priority(Priority.Scheduled);
            eipClient.setO_T_variableLength(false);
            eipClient.setO_T_connectionType(ConnectionType.Point_to_Point);

            //Parameters for Target -> Originator communication
            eipClient.setT_O_InstanceID(101);       //Input Assembly 65hex
            eipClient.setT_O_Length(16);
            eipClient.setT_O_RealTimeFormat(RealTimeFormat.Modeless);
            eipClient.setT_O_ownerRedundant(false);
            eipClient.setT_O_priority(Priority.Scheduled);
            eipClient.setT_O_variableLength(false);
            eipClient.setT_O_connectionType(ConnectionType.Point_to_Point);


            System.out.println("Send Forward open to initiate IO Messaging");
            eipClient.ForwardOpen();
            for (int i = 0; i < 50; i++)
            {
                eipClient.O_T_IOData[0] = (byte)(eipClient.O_T_IOData[0]+1);
                eipClient.O_T_IOData[1] = (byte)(eipClient.O_T_IOData[1]+1);
                eipClient.O_T_IOData[2] = (byte)(eipClient.O_T_IOData[2]+1);
                eipClient.O_T_IOData[3] = (byte)(eipClient.O_T_IOData[3]+1);

                System.out.println("Input Module 1: "+eipClient.T_O_IOData[8]);
                System.out.println("Input Module 2: "+eipClient.T_O_IOData[9]);
                System.out.println("Input Module 3: "+eipClient.T_O_IOData[10]);
                System.out.println("Input Module 4: "+eipClient.T_O_IOData[11]);
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
