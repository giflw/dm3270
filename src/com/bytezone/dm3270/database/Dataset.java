package com.bytezone.dm3270.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dataset
{
  private static final SimpleDateFormat fmt1 = new SimpleDateFormat ("yyyy/MM/dd");

  private final String name;

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

  public Dataset (String name)
  {
    this.name = name;
  }

  public String getName ()
  {
    return name;
  }

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

  public void setSpace (int tracks, int cylinders, int extents, int percent)
  {
    this.tracks = tracks;
    this.cylinders = cylinders;
    this.extents = extents;
    this.percent = percent;
  }

  public void setDisposition (String dsorg, String recfm, int lrecl, int blksize)
  {
    this.dsorg = dsorg;
    this.recfm = recfm;
    this.lrecl = lrecl;
    this.blksize = blksize;
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

  public void setDates (String created, String expires, String referred)
  {
    try
    {
      if (!created.isEmpty ())
      {
        this.created = fmt1.parse (created);
        this.createdSQL = new java.sql.Date (this.created.getTime ());
      }

      if (!expires.isEmpty ())
      {
        this.expires = fmt1.parse (expires);
        this.expiresSQL = new java.sql.Date (this.expires.getTime ());
      }

      if (!referred.isEmpty ())
      {
        this.referred = fmt1.parse (referred);
        this.referredSQL = new java.sql.Date (this.referred.getTime ());
      }
    }
    catch (ParseException e)
    {
      e.printStackTrace ();
    }
  }

  public boolean isPartitioned ()
  {
    return dsorg.equals ("PO");
  }

  @Override
  public String toString ()
  {
    return String
        .format ("%-3s %-31s  %3d %3d  %-6s  %-6s  %3d " + " %3d  %-4s %4d %6d  %s %s %s",
                 dsorg, name, tracks, cylinders, device, volume, extents, percent, recfm,
                 lrecl, blksize, catalog, fmt1.format (created), fmt1.format (referred));
  }
}