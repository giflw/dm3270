package com.bytezone.dm3270.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dataset
{
  private static final SimpleDateFormat fmt1 = new SimpleDateFormat ("yyyy/MM/dd");

  final String name;

  String volume;
  String device;
  String catalog;

  int tracks;
  int cylinders;
  int extents;
  int percent;

  String dsorg;
  String recfm;
  int lrecl;
  int blksize;

  Date created;
  Date expires;
  Date referred;

  java.sql.Date createdSQL;
  java.sql.Date expiresSQL;
  java.sql.Date referredSQL;

  // ---------------------------------------------------------------------------------//
  // Constructor
  // ---------------------------------------------------------------------------------//

  public Dataset (String name)
  {
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  // Location
  // ---------------------------------------------------------------------------------//

  public void setLocation (String volume, String device, String catalog)
  {
    this.volume = volume;
    this.device = device;
    this.catalog = catalog;
  }

  public void setVolume (String volume)
  {
    this.volume = volume;
  }

  public void setCatalog (String catalog)
  {
    this.catalog = catalog;
  }

  public void setDevice (String device)
  {
    this.device = device;
  }

  // ---------------------------------------------------------------------------------//
  // Space
  // ---------------------------------------------------------------------------------//

  public void setSpace (int tracks, int cylinders, int extents, int percent)
  {
    this.tracks = tracks;
    this.cylinders = cylinders;
    this.extents = extents;
    this.percent = percent;
  }

  // ---------------------------------------------------------------------------------//
  // Disposition
  // ---------------------------------------------------------------------------------//

  public void setDisposition (String dsorg, String recfm, int lrecl, int blksize)
  {
    this.dsorg = dsorg;
    this.recfm = recfm;
    this.lrecl = lrecl;
    this.blksize = blksize;
  }

  // ---------------------------------------------------------------------------------//
  // Dates
  // ---------------------------------------------------------------------------------//

  public void setDates (String created, String expires, String referred)
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
      if (!expires.trim ().isEmpty ())
      {
        this.expires = fmt1.parse (expires);
        this.expiresSQL = new java.sql.Date (this.expires.getTime ());
      }
    }
    catch (ParseException e)
    {
      System.out.printf ("Invalid expires date: [%s]%n", expires);
    }

    try
    {
      if (!referred.trim ().isEmpty ())
      {
        this.referred = fmt1.parse (referred);
        this.referredSQL = new java.sql.Date (this.referred.getTime ());
      }
    }
    catch (ParseException e)
    {
      System.out.printf ("Invalid referred date: [%s]%n", referred);
    }
  }

  public void setDates (java.sql.Date createdSQL, java.sql.Date expiresSQL,
      java.sql.Date referredSQL)
  {
    this.createdSQL = createdSQL;
    this.expiresSQL = expiresSQL;
    this.referredSQL = referredSQL;

    if (createdSQL != null)
      created = new Date (createdSQL.getTime ());
    if (expiresSQL != null)
      expires = new Date (expiresSQL.getTime ());
    if (referredSQL != null)
      referred = new Date (referredSQL.getTime ());
  }

  // ---------------------------------------------------------------------------------//
  // Merge
  // ---------------------------------------------------------------------------------//

  public void merge (Dataset oldDataset)
  {
    assert name.equals (oldDataset.name);

    System.out.println ("merging dataset: " + name);
  }

  // ---------------------------------------------------------------------------------//
  // Utility Methods
  // ---------------------------------------------------------------------------------//

  public String getName ()
  {
    return name;
  }

  public boolean isPartitioned ()
  {
    return dsorg.equals ("PO");
  }

  @Override
  public String toString ()
  {
    return String
        .format ("%-3s %-31s  %3d %3d  %-6s  %-6s  %3d  %3d  %-4s %4d %6d  %s %s %s",
                 dsorg, name, tracks, cylinders, device, volume, extents, percent, recfm,
                 lrecl, blksize, catalog, fmt1.format (created), fmt1.format (referred));
  }
}