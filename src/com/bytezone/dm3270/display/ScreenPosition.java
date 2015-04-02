package com.bytezone.dm3270.display;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import com.bytezone.dm3270.application.Utility;
import com.bytezone.dm3270.attributes.Attribute;
import com.bytezone.dm3270.attributes.StartFieldAttribute;
import com.bytezone.dm3270.orders.Order;

public class ScreenPosition
{
  // GraphicsEscape characters
  private static final byte TOP_LEFT = (byte) 0xC5;
  private static final byte TOP_RIGHT = (byte) 0xD5;
  private static final byte BOTTOM_LEFT = (byte) 0xC4;
  private static final byte BOTTOM_RIGHT = (byte) 0xD4;
  private static final byte HORIZONTAL_LINE = (byte) 0xA2;
  private static final byte VERTICAL_LINE = (byte) 0x85;

  private StartFieldAttribute startFieldAttribute;
  private final List<Attribute> attributes = new ArrayList<> ();

  private byte value;
  private boolean isGraphics;
  private boolean isVisible = true;
  private ScreenContext screenContext;
  private final ScreenContext baseContext;

  private final boolean expanded = true;
  private boolean topLine;        // testing
  private boolean bottomLine;     // testing

  private final CharacterSize characterSize;
  private final GraphicsContext gc;

  public ScreenPosition (GraphicsContext gc, CharacterSize characterSize,
      ScreenContext baseContext)
  {
    this.gc = gc;
    this.characterSize = characterSize;
    this.baseContext = baseContext;
    screenContext = baseContext;
  }

  public void addAttribute (Attribute attribute)
  {
    if (attribute instanceof StartFieldAttribute)
    {
      startFieldAttribute = (StartFieldAttribute) attribute;
      setVisible (false);
    }
    else
      attributes.add (attribute);
  }

  // Password fields etc
  public void setVisible (boolean visible)
  {
    this.isVisible = visible;
  }

  // All the colour and highlight options
  public void setScreenContext (ScreenContext screenContext)
  {
    this.screenContext = screenContext;
  }

  public boolean isStartField ()
  {
    return startFieldAttribute != null;
  }

  public boolean isGraphicsChar ()
  {
    return isGraphics;
  }

  public StartFieldAttribute getStartFieldAttribute ()
  {
    return startFieldAttribute;
  }

  public boolean hasAttributes ()
  {
    return attributes.size () > 0;
  }

  public List<Attribute> getAttributes ()
  {
    return attributes;
  }

  public void setChar (byte value)
  {
    this.value = value;
    isGraphics = false;
  }

  public void setGraphicsChar (byte value)
  {
    this.value = value;
    isGraphics = true;
  }

  public char getChar ()
  {
    if (value == 0 || isStartField ())
      return ' ';

    if (isGraphics)
      switch (value)
      {
        case HORIZONTAL_LINE:
          return '-';
        case VERTICAL_LINE:
          return '|';
        default:
          return '*';
      }

    return (char) Utility.ebc2asc[value & 0xFF];
  }

  public byte getByte ()
  {
    return value;
  }

  public boolean isNull ()
  {
    return value == 0;
  }

  public void reset (ScreenContext screenContext)
  {
    isVisible = true;
    value = 0;
    isGraphics = false;
    this.screenContext = screenContext;       // set to screen default
    startFieldAttribute = null;
    attributes.clear ();
  }

  public void reset ()
  {
    reset (baseContext);
  }

  public int pack (byte[] buffer, int ptr, byte order)
  {
    assert isStartField ();

    buffer[ptr++] = order;

    if (order == Order.START_FIELD)
      buffer[ptr++] = startFieldAttribute.getAttributeValue ();
    else if (order == Order.START_FIELD_EXTENDED)
    {
      buffer[ptr++] = (byte) (attributes.size () + 1);    // includes the SFA
      ptr = startFieldAttribute.pack (buffer, ptr);
      for (Attribute attribute : attributes)
        ptr = attribute.pack (buffer, ptr);
    }
    else
      System.out.println ("I should throw an exception here");

    return ptr;
  }

  public int pack (byte[] buffer, int ptr, byte[] replyTypes)
  {
    assert !isStartField ();

    for (Attribute attribute : attributes)
      if (attribute.matches (Attribute.XA_RESET) || attribute.matches (replyTypes))
      {
        buffer[ptr++] = Order.SET_ATTRIBUTE;
        ptr = attribute.pack (buffer, ptr);       // packs type/value pair
      }

    buffer[ptr++] = value;

    return ptr;
  }

  public void draw (double x, double y, boolean hasCursor)
  {
    double charWidth = characterSize.getWidth ();
    double charHeight = characterSize.getHeight ();
    double ascent = characterSize.getAscent ();

    // Draw background
    if (hasCursor)
    {
      gc.setFill (isVisible ? screenContext.reverseVideo ? screenContext.backgroundColor
          : screenContext.foregroundColor : screenContext.foregroundColor);
      if (expanded)
        gc.fillRect (x + .15, y + .15, charWidth - .3, charHeight - .3);
      else
        gc.fillRect (x + .15, y + .15, charWidth - .3, charHeight - .3);
    }
    else
    {
      gc.setFill (isVisible ? screenContext.reverseVideo ? screenContext.foregroundColor
          : screenContext.backgroundColor : screenContext.backgroundColor);
      if (expanded)
        gc.fillRect (x, y, charWidth + 1.4, charHeight + 1.4);
      else
        gc.fillRect (x, y, charWidth + .4, charHeight + .4);
    }

    // Draw foreground
    if (isVisible)
    {
      Color color;
      if (hasCursor)
        color =
            screenContext.reverseVideo ? screenContext.foregroundColor
                : screenContext.backgroundColor;
      else
        color =
            screenContext.reverseVideo ? screenContext.backgroundColor
                : screenContext.foregroundColor;

      if (isGraphics)
      {
        gc.setStroke (color);
        doGraphics (x, y);
      }
      else
      {
        gc.setFill (color);
        gc.fillText (getChar () + "", x, y + ascent);       // can we speed this up?
      }

      if (screenContext.underscore)
      {
        gc.setStroke (screenContext.foregroundColor);
        double y2 = y + charHeight;
        if (expanded)
          gc.strokeLine (x, y2, x + charWidth + 1.5, y2);
        else
          gc.strokeLine (x, y2, x + charWidth, y2);
      }

      if (topLine && expanded)
      {
        gc.setStroke (Color.WHITE);
        double y2 = y - 0.8;
        gc.strokeLine (x, y2, x + charWidth, y2);
      }

      if (bottomLine && expanded)
      {
        gc.setStroke (Color.WHITE);
        double y2 = y + charHeight + 0.8;
        gc.strokeLine (x, y2, x + charWidth, y2);
      }
    }
  }

  private void doGraphics (double x, double y)
  {
    double dx = 4.5;
    double extra = expanded ? 1 : 0;
    //    gc.setLineWidth (1.5);

    switch (value)
    {
      case HORIZONTAL_LINE:
        gc.strokeLine (x, y + 10, x + characterSize.getWidth () + extra, y + 10);
        break;

      case VERTICAL_LINE:
        gc.strokeLine (x + dx, y, x + dx, y + characterSize.getHeight () + 1);
        break;

      case TOP_LEFT:
        gc.strokeLine (x + dx, y + 11, x + dx, y + 20);
        gc.strokeLine (x + dx, y + 10, x + 10, y + 10);
        break;

      case TOP_RIGHT:
        gc.strokeLine (x + dx, y + 11, x + dx, y + 20);
        gc.strokeLine (x, y + 10, x + dx, y + 10);
        break;

      case BOTTOM_LEFT:
        gc.strokeLine (x + dx, y, x + dx, y + 10);
        gc.strokeLine (x + dx, y + 10, x + 10, y + 10);
        break;

      case BOTTOM_RIGHT:
        gc.strokeLine (x + dx, y, x + dx, y + 10);
        gc.strokeLine (x, y + 10, x + dx, y + 10);
        break;

      default:
        gc.fillText (getChar () + "", x, y + characterSize.getAscent ());
        System.out.printf ("Unknown graphics character: %02X%n", value);
    }
  }

  public void erase (GraphicsContext gc, double x, double y)
  {
    gc.setFill (screenContext.backgroundColor);
    if (expanded)
      gc.fillRect (x, y, characterSize.getWidth () + 1, characterSize.getHeight () + 1);
    else
      gc.fillRect (x, y, characterSize.getWidth (), characterSize.getHeight ());
  }

  @Override
  public String toString ()
  {
    StringBuilder text = new StringBuilder ();
    if (isStartField ())
      text.append (startFieldAttribute);

    for (Attribute attribute : attributes)
    {
      if (text.length () > 0)
        text.append ("\n       ");
      text.append (String.format ("%-30s", attribute));
    }
    return text.toString ();
  }
}