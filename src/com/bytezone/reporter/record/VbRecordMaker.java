package com.bytezone.reporter.record;

public class VbRecordMaker extends DefaultRecordMaker
{
  public VbRecordMaker (byte[] buffer)
  {
    super (buffer);
  }

  @Override
  protected void split ()
  {
    int ptr = 0;
    while (ptr < buffer.length)
    {
      int start = ptr;
      int filler = (buffer[ptr++] & 0xFF) << 8;
      filler |= buffer[ptr++] & 0xFF;
      if (filler != 0)
        System.out.println ("Non zero");

      int reclen = (buffer[ptr++] & 0xFF) << 8;
      reclen |= buffer[ptr++] & 0xFF;
      int reclen2 = Math.min (reclen - 4, buffer.length - ptr);

      Record record = new Record (buffer, ptr, reclen2, start, reclen);
      ptr += reclen2;

      records.add (record);
    }
  }
}