package com.bytezone.dm3270.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Member
{
  private static final SimpleDateFormat fmt1 = new SimpleDateFormat ("yyyy/MM/dd");
  private static final SimpleDateFormat fmt2 =
      new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss");

  final String name;
  Dataset dataset;

  String id;
  int size;
  int init;
  int mod;
  int vv;
  int mm;

  Date created;
  Date changed;
  java.sql.Date createdSQL;
  java.sql.Date changedSQL;

  // ---------------------------------------------------------------------------------//
  // Constructor
  // ---------------------------------------------------------------------------------//

  public Member (Dataset dataset, String name)
  {
    this.dataset = dataset;
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  // Id
  // ---------------------------------------------------------------------------------//

  public void setID (String id)
  {
    this.id = id;
  }

  // ---------------------------------------------------------------------------------//
  // Size
  // ---------------------------------------------------------------------------------//

  public void setSize (int size)
  {
    this.size = size;
  }

  public void setSize (int size, int init, int mod, int vv, int mm)
  {
    this.size = size;
    this.init = init;
    this.mod = mod;
    this.vv = vv;
    this.mm = mm;
  }

  // ---------------------------------------------------------------------------------//
  // Dates
  // ---------------------------------------------------------------------------------//

  public void setDates (String created, String changed)
  {
    try
    {
      if (!created.trim ().isEmpty ())
      {
        this.created = fmt1.parse (created);
        this.createdSQL = new java.sql.Date (this.created.getTime ());
      }
    }
    catch (ParseException e)
    {
      System.out.printf ("Invalid created date: [%s]%n", created);
    }

    try
    {
      if (!changed.trim ().isEmpty ())
      {
        this.changed = fmt2.parse (changed);
        this.changedSQL = new java.sql.Date (this.changed.getTime ());
      }
    }
    catch (ParseException e)
    {
      System.out.printf ("Invalid changed date: [%s]%n", changed);
    }
  }

  public void setDates (java.sql.Date createdSQL, java.sql.Date changedSQL)
  {
    this.createdSQL = createdSQL;
    if (createdSQL != null)
      created = new Date (createdSQL.getTime ());

    this.changedSQL = changedSQL;
    if (changedSQL != null)
      changed = new Date (changedSQL.getTime ());
  }

  // ---------------------------------------------------------------------------------//
  // Merge
  // ---------------------------------------------------------------------------------//

  public void merge (Member oldMember)
  {
    assert dataset.getName ().equals (oldMember.dataset.getName ());

    System.out.println ("merging members: " + name);
  }

  // ---------------------------------------------------------------------------------//
  // Utility Methods
  // ---------------------------------------------------------------------------------//

  public String getName ()
  {
    return name;
  }

  @Override
  public String toString ()
  {
    if (created != null && changed != null)
      return String.format ("%-8s  %3d  %-8s %4d  %2d  %2d  %2d %s %s", name, size, id,
                            init, mod, vv, mm, fmt1.format (created),
                            fmt2.format (changed));
    return String.format ("%-8s  %3d  %-8s %4d  %2d  %2d  %2d", name, size, id, init, mod,
                          vv, mm);
  }
}