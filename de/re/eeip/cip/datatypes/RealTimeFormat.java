package de.re.eeip.cip.datatypes;

/**
 * Created by sr555 on 30.09.17.
 */
public enum RealTimeFormat
{
    Modeless ((byte)0),
    ZeroLength ((byte)1),
    Heartbeat ((byte)2),
    Header32Bit ((byte)3);

    private final byte value;

    RealTimeFormat(byte value)
    {
        this.value = value;
    }

    public byte getValue()
    {
        return value;
    }
}
