package com.bytezone.reporter.text;

import com.bytezone.reporter.record.Record;

public interface TextMaker
{
  public String getText (byte[] buffer, int offset, int length);

  public boolean test (byte[] buffer, int offset, int length);

  public default boolean test (Record record)
  {
    return test (record.buffer, record.offset, record.length);
  }

  default void rightTrim (StringBuilder text)
  {
    while (text.length () > 0 && text.charAt (text.length () - 1) == ' ')
      text.deleteCharAt (text.length () - 1);
  }
}