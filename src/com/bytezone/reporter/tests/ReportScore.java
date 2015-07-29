package com.bytezone.reporter.tests;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.reporter.record.Record;
import com.bytezone.reporter.record.RecordMaker;
import com.bytezone.reporter.reports.Page;
import com.bytezone.reporter.reports.ReportMaker;
import com.bytezone.reporter.text.TextMaker;

import javafx.scene.control.Pagination;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ReportScore implements Comparable<ReportScore>
{
  private static Font font = Font.font ("Ubuntu Mono", FontWeight.NORMAL, 14);

  public final RecordMaker recordMaker;
  public final TextMaker textMaker;
  public final ReportMaker reportMaker;

  public final double score;
  public final int sampleSize;

  private final List<Page> pages = new ArrayList<> ();
  private Pagination pagination;
  private final TextArea textArea = new TextArea ();

  public ReportScore (RecordMaker recordMaker, TextMaker textMaker,
      ReportMaker reportMaker, double score, int sampleSize)
  {
    this.recordMaker = recordMaker;
    this.textMaker = textMaker;
    this.reportMaker = reportMaker;

    this.score = score;
    this.sampleSize = sampleSize;

    textArea.setFont (font);
    textArea.setEditable (false);
  }

  public boolean matches (RecordMaker recordMaker, TextMaker textMaker,
      ReportMaker reportMaker)
  {
    return this.recordMaker == recordMaker && this.textMaker == textMaker
        && this.reportMaker == reportMaker;
  }

  public List<Page> getPages ()
  {
    return pages;
  }

  public Pagination getPagination ()
  {
    return pagination;
  }

  public void paginate ()
  {
    if (pagination == null)
    {
      pagination = new Pagination ();
      pagination.setPageFactory (i -> getFormattedPage (i));

      reportMaker.createPages (this);
      pagination.setPageCount (pages.size ());
    }
  }

  public TextArea getFormattedPage (int pageNumber)
  {
    List<Record> records = recordMaker.getRecords ();

    StringBuilder text = new StringBuilder ();
    if (pageNumber < 0 || pageNumber >= pages.size ())
    {
      textArea.clear ();
      return textArea;
    }

    Page page = pages.get (pageNumber);
    int firstRecord = page.getFirstRecordIndex ();
    int firstRecordOffset = page.getFirstRecordOffset ();
    int lastRecord = page.getLastRecordIndex ();
    int lastRecordOffset = page.getLastRecordOffset ();

    System.out.println (page);

    if (firstRecord == lastRecord)
    {
      Record record = records.get (firstRecord);
      text.append (getSubrecord (record, firstRecordOffset, lastRecordOffset));
    }
    else
    {
      for (int i = firstRecord; i <= lastRecord; i++)
      {
        Record record = records.get (i);
        String formattedRecord = null;

        if (firstRecordOffset > 0 && i == firstRecord)
          formattedRecord = getSubrecord (record, firstRecordOffset, 0);
        else if (lastRecordOffset > 0 && i == lastRecord)
          formattedRecord = getSubrecord (record, 0, lastRecordOffset);
        else
          formattedRecord = (reportMaker.getFormattedRecord (this, record));

        if (formattedRecord == null)
          continue;

        text.append (formattedRecord);
        text.append ('\n');

        if (reportMaker.newlineBetweenRecords ())
          text.append ('\n');
      }
    }

    // remove trailing newlines
    while (text.length () > 0 && text.charAt (text.length () - 1) == '\n')
      text.deleteCharAt (text.length () - 1);

    textArea.setText (text.toString ());

    return textArea;
  }

  private String getSubrecord (Record record, int from, int to)
  {
    int offset = 0;
    int length = 0;

    if (from > 0 && to > 0)
    {
      offset = from;
      length = to - from;
    }
    else if (from > 0)
    {
      offset = from;
      length = record.length - from;
    }
    else if (to > 0)
    {
      offset = 0;
      length = to;
    }
    else
    {
      offset = 0;
      length = record.length;
    }
    return reportMaker.getFormattedRecord (this, record, offset, length);
  }

  public Page addPage (int firstRecord, int lastRecord)
  {
    Page page = new Page (recordMaker.getRecords (), firstRecord, lastRecord);
    pages.add (page);

    if (pages.size () > 1)
    {
      Page previousPage = pages.get (pages.size () - 2);
      page.setFirstRecordOffset (previousPage.getLastRecordOffset ());
    }

    return page;
  }

  @Override
  public int compareTo (ReportScore o)
  {
    return Double.compare (this.score, o.score);
  }

  @Override
  public String toString ()
  {
    return String.format ("%-10s %-10s %-10s %6.2f %3d  %s", recordMaker, textMaker,
                          reportMaker, score, sampleSize, pagination);
  }
}