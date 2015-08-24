package com.bytezone.reporter.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bytezone.reporter.application.TreePanel.FileNode;
import com.bytezone.reporter.file.ReportData;
import com.bytezone.reporter.file.ReportScore;
import com.bytezone.reporter.record.RecordMaker;
import com.bytezone.reporter.reports.ReportMaker;
import com.bytezone.reporter.text.TextMaker;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class FormatBox
{
  private final Set<PaginationChangeListener> paginationChangeListeners =
      new HashSet<> ();

  final ToggleGroup recordsGroup = new ToggleGroup ();
  final ToggleGroup encodingsGroup = new ToggleGroup ();
  final ToggleGroup reportsGroup = new ToggleGroup ();

  private final List<RadioButton> recordMakerButtons = new ArrayList<> ();
  private final List<RadioButton> textMakerButtons = new ArrayList<> ();
  private final List<RadioButton> reportMakerButtons = new ArrayList<> ();

  private final Label lblSizeText = new Label ();
  private final Label lblRecordsText = new Label ();

  private final VBox recordsBox;
  private final VBox encodingsBox;
  private final VBox reportsBox;

  private ReportData reportData;
  private final VBox formattingBox;
  private final Font monospacedFont = Font.font ("monospaced", 14);

  public FormatBox (PaginationChangeListener changeListener)
  {
    ReportData reportData = new ReportData ();

    // create a temporary ReportData to get the xxxMakers' names
    List<RecordMaker> recordMakers = reportData.getRecordMakers ();
    List<TextMaker> textMakers = reportData.getTextMakers ();
    List<ReportMaker> reportMakers = reportData.getReportMakers ();

    recordsBox = createVBox (recordMakers, recordMakerButtons, recordsGroup);
    encodingsBox = createVBox (textMakers, textMakerButtons, encodingsGroup);
    reportsBox = createVBox (reportMakers, reportMakerButtons, reportsGroup);

    formattingBox = createFormattingBox ();

    // set ReporterNode as a PaginationChangeListener
    addPaginationChangeListener (changeListener);
  }

  public VBox getPanel ()
  {
    return formattingBox;
  }

  // called from ReporterNode.nodeSelected()
  public void setFileNode (FileNode fileNode)
  {
    this.reportData = fileNode.getReportData ();
    if (!reportData.hasData ())
      reportData.addBuffer (fileNode);// create scores

    setUserData (recordMakerButtons, reportData.getRecordMakers ());
    setUserData (textMakerButtons, reportData.getTextMakers ());
    setUserData (reportMakerButtons, reportData.getReportMakers ());

    adjustButtons ();
    buttonSelected ();// force a pagination change
  }

  private void adjustButtons ()
  {
    disableAll (recordMakerButtons);
    disableAll (textMakerButtons);
    disableAll (reportMakerButtons);

    // Enable the buttons that have perfect scores
    for (ReportScore reportScore : reportData.getPerfectScores ())
    {
      enableButton (recordMakerButtons, reportScore.recordMaker);
      enableButton (textMakerButtons, reportScore.textMaker);
      enableButton (reportMakerButtons, reportScore.reportMaker);
    }

    // Find the best report possible and select its buttons
    selectButtons (reportData.getSelectedReportScore ());
  }

  private void disableAll (List<RadioButton> buttons)
  {
    for (RadioButton button : buttons)
      button.setDisable (true);
  }

  private void buttonSelected ()
  {
    RecordMaker recordMaker = getSelectedRecordMaker ();
    TextMaker textMaker = getSelectedTextMaker ();
    ReportMaker reportMaker = getSelectedReportMaker ();

    lblSizeText.setText (String.format ("%,10d", recordMaker.getBuffer ().length));
    lblRecordsText.setText (String.format ("%,10d", recordMaker.getRecords ().size ()));

    ReportScore reportScore =
        reportData.setReportScore (recordMaker, textMaker, reportMaker);

    if (reportScore != null)
      firePaginationChange (reportScore.getPagination ());
    else
      System.out.println ("no reportscore found");
  }

  private void selectButtons (ReportScore reportScore)
  {
    if (reportScore != null)
    {
      selectButton (recordMakerButtons, reportScore.recordMaker);
      selectButton (textMakerButtons, reportScore.textMaker);
      selectButton (reportMakerButtons, reportScore.reportMaker);
    }
    else
      System.out.println ("Imperfect ReportScore selected");
  }

  private void selectButton (List<RadioButton> buttons, Object userData)
  {
    for (RadioButton button : buttons)
      if (button.getUserData () == userData)
      {
        button.setSelected (true);
        return;
      }
  }

  private void enableButton (List<RadioButton> buttons, Object userData)
  {
    for (RadioButton button : buttons)
      if (button.getUserData () == userData)
      {
        button.setDisable (false);
        return;
      }
  }

  private RecordMaker getSelectedRecordMaker ()
  {
    return (RecordMaker) recordsGroup.getSelectedToggle ().getUserData ();
  }

  private TextMaker getSelectedTextMaker ()
  {
    return (TextMaker) encodingsGroup.getSelectedToggle ().getUserData ();
  }

  private ReportMaker getSelectedReportMaker ()
  {
    return (ReportMaker) reportsGroup.getSelectedToggle ().getUserData ();
  }

  private void firePaginationChange (Pagination pagination)
  {
    for (PaginationChangeListener listener : paginationChangeListeners)
      listener.paginationChanged (pagination);
  }

  private void addPaginationChangeListener (PaginationChangeListener listener)
  {
    paginationChangeListeners.add (listener);
  }

  private void removePaginationChangeListener (PaginationChangeListener listener)
  {
    paginationChangeListeners.remove (listener);
  }

  private VBox createFormattingBox ()
  {
    Label lblSize = setLabel ("Bytes", 60);
    Label lblRecords = setLabel ("Records", 60);
    lblSizeText.setFont (monospacedFont);
    lblRecordsText.setFont (monospacedFont);

    HBox hbox1 = new HBox (10);
    hbox1.getChildren ().addAll (lblSize, lblSizeText);

    HBox hbox2 = new HBox (10);
    hbox2.getChildren ().addAll (lblRecords, lblRecordsText);

    VBox vbox = new VBox (10);
    vbox.setPadding (new Insets (10));
    vbox.getChildren ().addAll (hbox1, hbox2);
    vbox.setPrefWidth (180);

    VBox formattingBox = new VBox ();
    addTitledPane (formattingBox, "Data size", vbox, true);
    addTitledPane (formattingBox, "Structure", recordsBox, false);
    addTitledPane (formattingBox, "Encoding", encodingsBox, false);
    addTitledPane (formattingBox, "Formatting", reportsBox, true);

    return formattingBox;
  }

  private TitledPane addTitledPane (VBox parent, String text, VBox contents,
      boolean expanded)
  {
    TitledPane titledPane = new TitledPane (text, contents);
    titledPane.setCollapsible (true);
    titledPane.setExpanded (expanded);
    parent.getChildren ().add (titledPane);
    return titledPane;
  }

  private VBox createVBox (List<? extends Object> objects, List<RadioButton> buttons,
      ToggleGroup group)
  {
    VBox vbox = new VBox (10);
    vbox.setPadding (new Insets (10));

    // List of RecordMaker/TextMaker/ReportMaker
    for (Object userData : objects)
    {
      RadioButton button = new RadioButton (userData.toString ());
      button.setToggleGroup (group);
      button.setOnAction (e -> buttonSelected ());

      buttons.add (button);
      vbox.getChildren ().add (button);
    }

    setUserData (buttons, objects);

    return vbox;
  }

  private void setUserData (List<RadioButton> buttons, List<? extends Object> objects)
  {
    assert buttons.size () == objects.size ();
    for (int i = 0; i < buttons.size (); i++)
      buttons.get (i).setUserData (objects.get (i));
  }

  private Label setLabel (String text, int width)
  {
    Label label = new Label (text);
    label.setPrefWidth (width);
    return label;
  }

  @Override
  public String toString ()
  {
    StringBuilder text = new StringBuilder ();

    RadioButton btnRecord = (RadioButton) recordsGroup.getSelectedToggle ();
    if (btnRecord == null)
      text.append ("Nothing selected yet");
    else
    {
      text.append (String.format ("Record maker ..... %s%n", btnRecord.getUserData ()));
      RadioButton btnEncoding = (RadioButton) encodingsGroup.getSelectedToggle ();
      text.append (String.format ("Encoding ......... %s%n", btnEncoding.getUserData ()));
      RadioButton btnReport = (RadioButton) reportsGroup.getSelectedToggle ();
      text.append (String.format ("Report maker ..... %s%n", btnReport.getUserData ()));
    }

    return text.toString ();
  }
}