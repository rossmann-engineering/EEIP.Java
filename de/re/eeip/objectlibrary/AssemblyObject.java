package de.re.eeip.objectlibrary;

import de.re.eeip.cip.exception.CIPException;

import java.io.IOException;

/**
 * Created by sr555 on 06.10.17.
 */
public class AssemblyObject
{
    public de.re.eeip.EEIPClient eeipClient;


    /**
     * Constructor
     * @param eeipClient    EEIPClient Object
     */
    public AssemblyObject(de.re.eeip.EEIPClient eeipClient)
    {
        this.eeipClient = eeipClient;
    }

    /**
     * Reads the Instance of the Assembly Object (Instance 101 returns the bytes of the class ID 101)
     * @param instanceNo    Instance number to be returned
     * @return              bytes of the Instance
     * @throws IOException
     * @throws CIPException
     */
    public byte[] getInstance(int instanceNo) throws IOException, CIPException {

        byte[] byteArray = eeipClient.GetAttributeSingle(4, instanceNo, 3);
        return byteArray;
    }

    /**
     * Sets an Instance of the Assembly Object
     * @param instanceNo    Sets an Instance of the Assembly Object
     * @param value         bytes of the Instance
     * @throws IOException
     * @throws CIPException
     */
    public void setInstance(int instanceNo, byte[] value) throws IOException, CIPException {

        eeipClient.SetAttributeSingle(4, instanceNo, 3, value);
    }


}
