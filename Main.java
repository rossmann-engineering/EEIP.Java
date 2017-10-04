import de.re.eeip.EEIPClient;
import de.re.eeip.cip.datatypes.ConnectionType;
import de.re.eeip.cip.datatypes.Priority;
import de.re.eeip.cip.datatypes.RealTimeFormat;
import de.re.eeip.cip.exception.CIPException;

import java.io.Console;
import java.io.IOException;
import java.util.List;

/**
 * Created by sr555 on 27.09.17.
 */
public class Main
{
    public static void main(String[] args) throws InterruptedException {
        EEIPClient eipClient = new EEIPClient();
        List<de.re.eeip.encapsulation.CipIdentityItem> cipIdentityItem = eipClient.ListIdentity();

        for (de.re.eeip.encapsulation.CipIdentityItem item : cipIdentityItem)
        {
            System.out.println("Ethernet/IP Device Found:");
            System.out.println(item.ProductName1);
            System.out.println("IP-Address: " + de.re.eeip.encapsulation.CipIdentityItem.getIPAddress(item.SocketAddress.SIN_Address));
            System.out.println("Port: " + item.SocketAddress.SIN_port);
            System.out.println("Vendor ID: " + item.VendorID1);
            System.out.println("Product-code: " + item.ProductCode1);
            System.out.println("Type-Code: " + item.ItemTypeCode);
            System.out.println("Serial Number: " + item.SerialNumber1);
        }


        try {
            eipClient.RegisterSession("192.168.178.66");

            System.out.println("Digitale Eingang1 #1 "+de.re.eeip.EEIPClient.ToUshort(eipClient.GetAttributeSingle(0x04,108,3)));

            eipClient.SetAttributeSingle(0x04,101,3, new byte[] {0x01});

            eipClient.setO_T_InstanceID(101);
            eipClient.setO_T_Length(1);
            eipClient.setO_T_RealTimeFormat(RealTimeFormat.Header32Bit);
            eipClient.setO_T_ownerRedundant(false);
            eipClient.setO_T_priority(Priority.Scheduled);
            eipClient.setO_T_variableLength(false);
            eipClient.setO_T_connectionType(ConnectionType.Point_to_Point);

            eipClient.setT_O_InstanceID(104);
            eipClient.setT_O_Length(3);
            eipClient.setT_O_RealTimeFormat(RealTimeFormat.Modeless);
            eipClient.setT_O_ownerRedundant(false);
            eipClient.setT_O_priority(Priority.Scheduled);
            eipClient.setT_O_variableLength(false);
            eipClient.setT_O_connectionType(ConnectionType.Point_to_Point);

            eipClient.ForwardOpen();
            for (int i = 0; i < 50; i++)
            {
                System.out.println(eipClient.T_O_IOData[0]);
                Thread.sleep(500);
            }
            eipClient.ForwardClose();



            eipClient.UnRegisterSession();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CIPException e) {
            e.printStackTrace();
        }

    }
}
