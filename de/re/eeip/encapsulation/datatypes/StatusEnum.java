package de.re.eeip.encapsulation.datatypes;

/**
 * Table 2-3.3 Error Codes
 */
public enum StatusEnum
{
    Success (0x0000),
    InvalidCommand (0x0001),
    InsufficientMemory (0x0002),
    IncorrectData (0x0003),
    InvalidSessionHandle (0x0064),
    InvalidLength (0x0065),
    UnsupportedEncapsulationProtocol (0x0069);

    private final int value;

    StatusEnum(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }
}
