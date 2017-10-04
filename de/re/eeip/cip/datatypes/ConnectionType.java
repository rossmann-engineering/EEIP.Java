package de.re.eeip.cip.datatypes;

/**
 * Created by sr555 on 30.09.17.
 */
public enum ConnectionType
{
    Null ((byte)0),
    Multicast ((byte)1),
    Point_to_Point ((byte)2);

    private final byte value;

    ConnectionType(byte value)
    {
        this.value = value;
    }

    public byte getValue()
    {
        return value;
    }
}
