package de.re.eeip.cip.datatypes;

/**
 * Created by sr555 on 30.09.17.
 */
public enum Priority {
    Low ((byte)0),
    High ((byte)1),
    Scheduled ((byte)2),
    Urgent ((byte)3);

    private final byte value;

    Priority(byte value)
    {
        this.value = value;
    }

    public byte getValue()
    {
        return value;
    }
}
