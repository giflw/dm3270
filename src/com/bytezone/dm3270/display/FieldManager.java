package com.bytezone.dm3270.display;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.dm3270.plugins.PluginData;
import com.bytezone.dm3270.plugins.ScreenField;

public class FieldManager
{
  private static final String[] tsoMenus =
      { "Menu", "List", "Mode", "Functions", "Utilities", "Help" };

  private final Screen screen;
  private final ScreenContext baseContext;

  private final List<Field> fields = new ArrayList<> ();
  private final List<Field> unprotectedFields = new ArrayList<> ();
  private final List<Field> emptyFields = new ArrayList<> ();

  private int dataPositions;
  private int inputPositions;
  private int hiddenProtectedFields;
  private int hiddenUnprotectedFields;

  private String datasetsMatching;
  private String datasetsOnVolume;
  private Field tsoCommandField;
  private boolean isTSOCommandScreen;
  private boolean isDatasetList;
  private String highLevelQualifier;
  private String currentDataset;

  public FieldManager (Screen screen, ScreenContext baseContext)
  {
    this.screen = screen;
    this.baseContext = baseContext;
  }

  // this is called after the pen and screen positions have been modified
  public void buildFields ()
  {
    fields.clear ();
    unprotectedFields.clear ();
    emptyFields.clear ();

    dataPositions = 0;
    inputPositions = 0;
    hiddenProtectedFields = 0;
    hiddenUnprotectedFields = 0;

    List<ScreenPosition> positions = new ArrayList<ScreenPosition> ();

    int start = -1;
    int first = -1;
    int ptr = 0;

    while (ptr != first)                   // not wrapped around to the first field yet
    {
      ScreenPosition screenPosition = screen.getScreenPosition (ptr);

      // check for the start of a new field
      if (screenPosition.isStartField ())
      {
        if (start >= 0)
        // if there is a field to add
        {
          addField (new Field (screen, positions));
          positions.clear ();
        }
        else
          first = ptr;                     // this is the first field on the screen

        start = ptr;                       // beginning of the current field
      }

      // add ScreenPosition to the current field
      if (start >= 0)
        // if we are in a field...
        positions.add (screenPosition);     // collect next field's positions

      // increment ptr and wrap around
      if (++ptr == screen.screenSize)
      {           // faster than validate()
        ptr = 0;
        if (first == -1)
          break;                          // wrapped around and still no fields
      }
    }

    if (start >= 0 && positions.size () > 0)
      addField (new Field (screen, positions));

    assert(dataPositions + fields.size () == 1920) || fields.size () == 0;

    // build screen contexts for every position and link uprotected fields
    Field previousUnprotectedField = null;
    for (Field field : fields)
    {
      field.setScreenContexts (baseContext);
      if (field.isUnprotected ())
      {
        unprotectedFields.add (field);
        if (previousUnprotectedField != null)
          previousUnprotectedField.linkToNext (field);
        previousUnprotectedField = field;
      }
    }

    if (unprotectedFields.size () > 0)
    {
      // link first unprotected field to the last one
      Field firstField = unprotectedFields.get (0);
      Field lastField = unprotectedFields.get (unprotectedFields.size () - 1);
      lastField.linkToNext (firstField);

      // link protected fields to unprotected fields
      Field prev = lastField;
      Field next = firstField;

      for (Field field : fields)
        if (field.isProtected ())
        {
          field.setNext (next);
          field.setPrevious (prev);
        }
        else
        {
          next = field.getNextUnprotectedField ();
          prev = field;
        }
    }

    // getMenus ();
    checkTSOCommandField ();
  }

  private void addField (Field field)
  {
    fields.add (field);

    dataPositions += field.getDisplayLength ();

    if (field.getDisplayLength () == 0)
      emptyFields.add (field);

    if (field.isUnprotected ())
      inputPositions += field.getDisplayLength ();

    if (field.isHidden ())
      if (field.isProtected ())
        ++hiddenProtectedFields;
      else
        ++hiddenUnprotectedFields;
  }

  public Field getField (int position)      // this needs to be improved
  {
    for (Field field : fields)
      if (field.contains (position))
        return field;
    return null;
  }

  public List<Field> getUnprotectedFields ()
  {
    return unprotectedFields;
  }

  public List<Field> getFields ()
  {
    return fields;
  }

  public int size ()
  {
    return fields.size ();
  }

  Field eraseAllUnprotected ()
  {
    if (unprotectedFields.size () == 0)
      return null;

    for (Field field : unprotectedFields)
      field.clear (true);

    return unprotectedFields.get (0);
  }

  // ---------------------------------------------------------------------------------//
  // Convert internal Fields to ScreenFields for use by plugins
  // ---------------------------------------------------------------------------------//

  public PluginData getPluginScreen (int sequence, int row, int column)
  {
    List<ScreenField> screenFields = new ArrayList<> ();
    int count = 0;

    for (Field field : fields)
      screenFields.add (field.getScreenField (sequence, count++));

    return new PluginData (sequence, row, column, screenFields);
  }

  // ---------------------------------------------------------------------------------//
  // Interpret screen
  // ---------------------------------------------------------------------------------//

  public List<String> getMenus ()
  {
    List<String> menus = new ArrayList<> ();

    for (Field field : fields)
    {
      if (field.getFirstLocation () >= screen.columns)
        break;

      if (field.isProtected () && field.isVisible () && field.getDisplayLength () > 1)
      {
        String text = field.getText ().trim ();
        if (!text.isEmpty ())
          menus.add (text);
      }
    }

    return menus;
  }

  public Field getTSOCommandField ()
  {
    return tsoCommandField;
  }

  public boolean isTSOCommandScreen ()
  {
    return isTSOCommandScreen;
  }

  public String getCurrentDataset ()
  {
    return currentDataset;
  }

  private void checkTSOCommandField ()
  {
    int maxLocation = screen.columns * 5 + 20;
    int minLocation = screen.columns;
    boolean promptFound = false;
    tsoCommandField = null;

    for (Field field : fields)
    {
      if (field.getFirstLocation () > maxLocation)
        break;

      if (field.getFirstLocation () < minLocation)
        continue;

      int length = field.getDisplayLength ();

      if (promptFound)
      {
        if (field.isProtected () || field.isHidden ())
          break;

        if (length < 48 || (length > 70 && length != 234))
          break;

        tsoCommandField = field;
        break;
      }

      int column = field.getFirstLocation () % screen.columns;
      if (column > 2)
        continue;

      if (field.isUnprotected () || field.isHidden () || length < 4 || length > 15)
        continue;

      String text = field.getText ();

      if (text.endsWith ("===>"))
        promptFound = true;             // next loop iteration will return the field
    }

    isTSOCommandScreen = checkTSOCommandScreen ();
    checkDatasets ();

    currentDataset = "";
    checkEditDataset ();

    if (currentDataset.isEmpty ())
      checkBrowseDataset ();

    if (currentDataset.isEmpty ())
      checkViewDataset ();
  }

  private boolean checkTSOCommandScreen ()
  {
    if (fields.size () < 14)
      return false;

    Field field = fields.get (10);
    if (!"ISPF Command Shell".equals (field.getText ()))
      return false;

    int workstationFieldNo = 13;
    field = fields.get (workstationFieldNo);
    if (!"Enter TSO or Workstation commands below:".equals (field.getText ()))
    {
      ++workstationFieldNo;
      field = fields.get (workstationFieldNo);
      if (!"Enter TSO or Workstation commands below:".equals (field.getText ()))
        return false;
    }

    List<String> menus = getMenus ();
    if (menus.size () != tsoMenus.length)
      return false;

    int i = 0;
    for (String menu : menus)
      if (!tsoMenus[i++].equals (menu))
        return false;

    field = fields.get (workstationFieldNo + 5);
    if (field.getDisplayLength () != 234)
      return false;

    return true;
  }

  private void checkDatasets ()
  {
    isDatasetList = false;
    datasetsOnVolume = "";
    datasetsMatching = "";

    if (fields.size () < 19)
      return;

    Field field = fields.get (9);
    int location = field.getFirstLocation ();
    if (location != 161)
      return;

    String text = field.getText ();
    if (!text.startsWith ("DSLIST - Data Sets "))
      return;

    field = fields.get (11);
    location = field.getFirstLocation ();
    if (location != 241)
      return;
    if (!field.getText ().equals ("Command ===>"))
      return;

    field = fields.get (18);
    int pos = text.indexOf ("Row ");
    String category = text.substring (19, (pos > 0 ? pos : 64)).trim ();

    if (category.startsWith ("Matching"))
    {
      datasetsMatching = category.substring (9);
      // System.out.printf ("Datasets matching [%s]%n", datasetsMatching);
    }
    else if (category.startsWith ("on volume "))
    {
      datasetsOnVolume = category.substring (10).trim ();
      // System.out.printf ("Datasets on volume [%s]%n", datasetsOnVolume);
    }
    else
      System.out.println ("Unknown category: " + category);

    isDatasetList = true;
  }

  private void checkEditDataset ()
  {
    if (fields.size () < 13)
      return;

    Field field = fields.get (11);
    int location = field.getFirstLocation ();
    if (location != 161)
      return;

    String text = field.getText ().trim ();
    if (!text.equals ("EDIT"))
      return;

    field = fields.get (12);
    location = field.getFirstLocation ();
    if (location != 172)
      return;

    text = field.getText ().trim ();
    int pos = text.indexOf (' ');
    if (pos > 0)
    {
      String dataset = text.substring (0, pos);
      currentDataset = dataset;
    }
  }

  private void checkBrowseDataset ()
  {
    if (fields.size () < 8)
      return;

    Field field = fields.get (7);
    int location = field.getFirstLocation ();
    if (location != 161)
      return;

    String text = field.getText ();
    if (!text.equals ("BROWSE   "))
      return;

    field = fields.get (8);
    location = field.getFirstLocation ();
    if (location != 171)
      return;

    text = field.getText ().trim ();
    int pos = text.indexOf (' ');
    if (pos > 0)
    {
      String dataset = text.substring (0, pos);
      currentDataset = dataset;
    }
  }

  private void checkViewDataset ()
  {
    if (fields.size () < 13)
      return;

    Field field = fields.get (11);
    int location = field.getFirstLocation ();
    if (location != 161)
      return;

    String text = field.getText ().trim ();
    if (!text.equals ("VIEW"))
      return;

    field = fields.get (12);
    location = field.getFirstLocation ();
    if (location != 172)
      return;

    text = field.getText ().trim ();
    int pos = text.indexOf (' ');
    if (pos > 0)
    {
      String dataset = text.substring (0, pos);
      currentDataset = dataset;
    }
  }

  // ---------------------------------------------------------------------------------//
  // Debugging
  // ---------------------------------------------------------------------------------//

  public String getTotalsText ()
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Start fields     : %4d%n", fields.size ()));
    text.append (String.format ("  Zero length    : %4d%n", emptyFields.size ()));
    text.append (String.format ("  Unprotected    : %4d   (%d hidden)%n",
                                unprotectedFields.size (), hiddenUnprotectedFields));
    text.append (String.format ("  Protected      : %4d   (%d hidden)%n%n",
                                fields.size () - unprotectedFields.size (),
                                hiddenProtectedFields));

    text.append (String.format ("Screen positions : %4d%n",
                                dataPositions + fields.size ()));
    text.append (String.format ("  Attributes     : %4d%n", fields.size ()));
    text.append (String.format ("  Output         : %4d%n",
                                dataPositions - inputPositions));
    text.append (String.format ("  Input          : %4d", inputPositions));

    return text.toString ();
  }

  public String getFieldsText ()
  {
    StringBuilder text = new StringBuilder ();

    int count = 0;
    for (Field field : fields)
      text.append (String.format ("%4d %s%n", count++, field));

    if (text.length () > 0)
      text.deleteCharAt (text.length () - 1);

    return text.toString ();
  }
}