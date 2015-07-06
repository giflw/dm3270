package com.bytezone.reporter.record;

public class RdwRecordMaker extends DefaultRecordMaker
{
  public RdwRecordMaker (byte[] buffer)
  {
    super (buffer);
  }

  @Override
  protected void split ()
  {
    int ptr = 0;
    while (ptr < buffer.length)
    {
      int reclen = (buffer[ptr++] & 0xFF) << 8;
      reclen |= buffer[ptr++] & 0xFF;

      int filler = (buffer[ptr++] & 0xFF) << 8;
      filler |= buffer[ptr++] & 0xFF;
      if (filler != 0)
        System.out.println ("Non zero");

      reclen = Math.min (reclen - 4, buffer.length - ptr);

      byte[] record = new byte[reclen];
      System.arraycopy (buffer, ptr, record, 0, reclen);
      ptr += reclen;

      records.add (record);
    }
  }

  @Override
  protected void fastSplit ()
  {
    int ptr = 0;
    while (ptr < buffer.length)
    {
      int start = ptr;
      int reclen = (buffer[ptr++] & 0xFF) << 8;
      reclen |= buffer[ptr++] & 0xFF;

      int filler = (buffer[ptr++] & 0xFF) << 8;
      filler |= buffer[ptr++] & 0xFF;
      if (filler != 0)
        System.out.println ("Non zero");

      int reclen2 = Math.min (reclen - 4, buffer.length - ptr);

      Record record = new Record (buffer, ptr, reclen2, start, reclen);
      ptr += reclen2;

      fastRecords.add (record);
    }
  }
}