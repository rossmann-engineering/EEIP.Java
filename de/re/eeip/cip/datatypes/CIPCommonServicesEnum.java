package de.re.eeip.cip.datatypes;

/**
 *  Table A-3.1 Volume 1 Chapter A-3
 */
public enum CIPCommonServicesEnum
{
    Get_Attributes_All ((byte)0x01),
    Set_Attributes_All_Request ((byte)0x02),
    Get_Attribute_List ((byte)0x03),
    Set_Attribute_List ((byte)0x04),
    Reset ((byte)0x05),
    Start ((byte)0x06),
    Stop ((byte)0x07),
    Create ((byte)0x08),
    Delete ((byte)0x09),
    Multiple_Service_Packet ((byte)0x0A),
    Apply_Attributes ((byte)0x0D),
    Get_Attribute_Single ((byte)0x0E),
    Set_Attribute_Single ((byte)0x10),
    Find_Next_Object_Instance ((byte)0x11),
    Error_Response ((byte)0x14),
    Restore ((byte)0x15),
    Save ((byte)0x16),
    NOP ((byte)0x17),
    Get_Member ((byte)0x18),
    Set_Member ((byte)0x19),
    Insert_Member ((byte)0x1A),
    Remove_Member ((byte)0x1B),
    GroupSync ((byte)0x1C);


    private final byte value;

    CIPCommonServicesEnum(byte value)
    {
    this.value = value;
    }

    public byte getValue()
    {
        return value;
    }

}
