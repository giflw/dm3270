package com.bytezone.reporter.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bytezone.reporter.file.RecordTester;
import com.bytezone.reporter.file.ReportScore;
import com.bytezone.reporter.record.CrRecordMaker;
import com.bytezone.reporter.record.CrlfRecordMaker;
import com.bytezone.reporter.record.FbRecordMaker;
import com.bytezone.reporter.record.LfRecordMaker;
import com.bytezone.reporter.record.NvbRecordMaker;
import com.bytezone.reporter.record.RavelRecordMaker;
import com.bytezone.reporter.record.RdwRecordMaker;
import com.bytezone.reporter.record.RecordMaker;
import com.bytezone.reporter.record.SingleRecordMaker;
import com.bytezone.reporter.record.VbRecordMaker;
import com.bytezone.reporter.reports.AsaReport;
import com.bytezone.reporter.reports.HexReport;
import com.bytezone.reporter.reports.NatloadReport;
import com.bytezone.reporter.reports.ReportMaker;
import com.bytezone.reporter.reports.TextReport;
import com.bytezone.reporter.text.AsciiTextMaker;
import com.bytezone.reporter.text.EbcdicTextMaker;
import com.bytezone.reporter.text.TextMaker;

public class ReportData
{
  private final List<RecordMaker> recordMakers;
  private final List<TextMaker> textMakers;
  private final List<ReportMaker> reportMakers;
  private final List<ReportScore> scores;

  private byte[] buffer;

  public ReportData ()
  {
    recordMakers = new ArrayList<> (Arrays
        .asList (new SingleRecordMaker (), new CrlfRecordMaker (), new CrRecordMaker (),
                 new LfRecordMaker (), new VbRecordMaker (), new RdwRecordMaker (),
                 new NvbRecordMaker (), new RavelRecordMaker (), new FbRecordMaker (63),
                 new FbRecordMaker (80), new FbRecordMaker (132),
                 new FbRecordMaker (252)));
    textMakers =
        new ArrayList<> (Arrays.asList (new AsciiTextMaker (), new EbcdicTextMaker ()));
    reportMakers = new ArrayList<> (
        Arrays.asList (new HexReport (true, true), new TextReport (false, false),
                       new AsaReport (false, true), new NatloadReport (false, false)));
    scores = new ArrayList<> ();
  }

  public boolean hasData ()
  {
    return buffer != null;
  }

  void readFile (File file) throws IOException
  {
    assert buffer == null;
    addBuffer (Files.readAllBytes (file.toPath ()));
  }

  void addBuffer (byte[] buffer)
  {
    this.buffer = buffer;

    for (RecordMaker recordMaker : recordMakers)
      recordMaker.setBuffer (buffer);

    List<RecordTester> testers = new ArrayList<> ();
    for (RecordMaker recordMaker : recordMakers)
      if (recordMaker instanceof FbRecordMaker)
      {
        int recordLength = ((FbRecordMaker) recordMaker).getRecordLength ();
        int fileLength = recordMaker.getBuffer ().length;
        if (fileLength % recordLength == 0)
          if (recordLength < 80)
            testers.add (new RecordTester (recordMaker, 30 * recordLength));
          else
            testers.add (new RecordTester (recordMaker, 10 * recordLength));
      }
      else
      {
        RecordTester recordTester = new RecordTester (recordMaker, 1024);
        if (recordTester.getSampleSize () > 2 || recordMaker instanceof SingleRecordMaker)
          testers.add (recordTester);
      }

    for (RecordTester tester : testers)
    {
      for (TextMaker textMaker : textMakers)
        tester.testTextMaker (textMaker);

      TextMaker textMaker = tester.getPreferredTextMaker ();

      for (ReportMaker reportMaker : reportMakers)
      {
        ReportScore score = tester.testReportMaker (reportMaker, textMaker);
        scores.add (score);
      }
    }
  }

  List<ReportScore> getScores ()
  {
    return scores;
  }

  List<RecordMaker> getRecordMakers ()
  {
    return recordMakers;
  }

  List<TextMaker> getTextMakers ()
  {
    return textMakers;
  }

  List<ReportMaker> getReportMakers ()
  {
    return reportMakers;
  }
}