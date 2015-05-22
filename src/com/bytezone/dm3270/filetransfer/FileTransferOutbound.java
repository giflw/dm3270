package com.bytezone.dm3270.filetransfer;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.dm3270.application.Utility;
import com.bytezone.dm3270.display.Screen;
import com.bytezone.dm3270.structuredfields.StructuredField;

public class FileTransferOutbound extends StructuredField
{
  private final byte rectype;
  private final byte subtype;

  private byte[] transfer;
  private boolean ebcdic;
  private String message;
  //  private DataHeader header;
  private final List<DataRecord> extraBytes = new ArrayList<> ();

  public FileTransferOutbound (byte[] buffer, int offset, int length, Screen screen)
  {
    super (buffer, offset, length, screen);
    assert data[0] == (byte) 0xD0;

    rectype = data[1];
    subtype = data[2];

    switch (rectype)
    {
      case 0x00:
        if (data.length == 33)
        {
          message = new String (data, 26, 7);
          extraBytes.add (new DataRecord (data, 3));
          extraBytes.add (new DataRecord (data, 9));
          extraBytes.add (new DataRecord (data, 19));
        }
        else if (data.length == 39)
        {
          message = new String (data, 32, 7);
          extraBytes.add (new DataRecord (data, 3));
          extraBytes.add (new DataRecord (data, 9));
          extraBytes.add (new DataRecord (data, 19));
          extraBytes.add (new DataRecord (data, 24));
        }
        else
          System.out.printf ("Unrecognised data length: %d%n", data.length);
        break;

      case 0x41:
        if (data.length != 3)
          System.out.printf ("Unrecognised data length: %d%n", data.length);
        break;

      case 0x45:
        extraBytes.add (new DataRecord (data, 3));
        extraBytes.add (new DataRecord (data, 8));
        if (data.length != 13)
          System.out.printf ("Unrecognised data length: %d%n", data.length);
        break;

      case 0x46:
        extraBytes.add (new DataRecord (data, 3));
        if (data.length != 7)
          System.out.printf ("Unrecognised data length: %d%n", data.length);
        break;

      case 0x47:
        if (subtype == 0x04)                  // message or transfer buffer
        {
          DataHeader header = new DataHeader (data, 3);
          extraBytes.add (header);
          int buflen = header.bufferLength - 5;
          if (data[6] == 0)                   // temp
            message = new String (data, 8, buflen).trim ();
          else
          {
            ebcdic = checkEbcdic (data, 8, buflen);
            transfer = new byte[buflen];
            System.arraycopy (data, 8, transfer, 0, buflen);
          }
        }
        else if (subtype == 0x11)             // transfer buffer
          extraBytes.add (new DataRecord (data, 3));
        else
        {
          if (data.length > 3)
            extraBytes.add (new DataRecord (data, 3));
          System.out.println ("Unknown subtype");
        }
        break;

      default:
        extraBytes.add (new DataRecord (data, 3));
        System.out.printf ("Unknown type: %02X%n", rectype);
    }

    if (true)
    {
      System.out.println (this);
      System.out.println ("-----------------------------------------"
          + "------------------------------");
    }
  }

  private boolean checkEbcdic (byte[] data, int offset, int length)
  {
    int ascii = 0;
    int ebcdic = 0;

    while (length > 0)
    {
      if (data[offset] == 0x20)
        ascii++;
      else if (data[offset] == 0x40)
        ebcdic++;
      length--;
      offset++;
    }
    return ebcdic > ascii;
  }

  @Override
  public void process ()
  {
    switch (rectype)
    {
      case 0x00:                      // OPEN request
        if (subtype == 0x12)
        {
          // RSF 0xD0 0x00 0x09
        }
        break;

      case 0x41:                      // CLOSE request
        if (subtype == 0x12)
        {
          // RSF 0xD0 0x41 0x09         CLOSE acknowledgement
        }
        break;

      case 0x45:                      // SET CURSOR request
        if (subtype == 0x11)
        {

        }
        break;

      case 0x46:                      // GET request
        if (subtype == 0x11)
        {
          // RSF 0xD0 0x46 0x05 0x63 0x06 0x00 0x00 0x00 0x01
          //     0xC0 0x80 0x61 
          // 2 length bytes
          // buffer of data 
          // (if CR/LF 0x0D/0x0A terminate with ctrl-z 0x1A)
          //
          // or
          // RSF 0xD0 0x46 0x08 0x69 0x04 0x22 0x00
        }
        break;

      case 0x47:                      // data transfer buffer
        if (subtype == 0x04)
        {
          // RSF  0xD0 0x47 0x05 0x63 0x06 0x00 0x00 0x00 0x01 
        }
        else if (subtype == 0x11)     // INSERT request
        {

        }
        break;
    }
  }

  @Override
  public String toString ()
  {
    StringBuilder text = new StringBuilder ("Struct Field : D0 File Transfer Outbound\n");
    text.append (String.format ("   type      : %02X%n", rectype));
    text.append (String.format ("   subtype   : %02X", subtype));

    for (DataRecord extra : extraBytes)
      text.append (String.format ("\n   %s", extra));

    if (message != null)
      text.append (String.format ("\n   message   : %s", message));

    if (transfer != null)
    {
      text.append ("\n");
      text.append (Utility.toHex (transfer, ebcdic));
    }

    return text.toString ();
  }
}