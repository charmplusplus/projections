# Makefile-ized "compile" script for
# projections-java.
# Converted by Orion Lawlor, 9/9/1999
#

SRC=\
    projections/analysis/AsciiIntegerReader.java\
    projections/analysis/Chare.java\
    projections/analysis/Entry.java\
    projections/analysis/LogEntry.java\
    projections/analysis/LogLoader.java\
    projections/analysis/LogReader.java\
    projections/analysis/PackTime.java\
    projections/analysis/ProjDefs.java\
    projections/analysis/StsReader.java\
    projections/analysis/SumAnalyzer.java\
    projections/analysis/TimelineEvent.java\
    projections/analysis/TimelineMessage.java\
    projections/analysis/UsageCalc.java\
    projections/analysis/UsageInterval.java\
    projections/analysis/ViewerEvent.java\
    projections/gui/AboutDialog.java\
    projections/gui/Analysis.java\
    projections/gui/AnimationColorBarPanel.java\
    projections/gui/AnimationDisplayPanel.java\
    projections/gui/AnimationWindow.java\
    projections/gui/BItem.java\
    projections/gui/Bubble.java\
    projections/gui/ColorMap.java\
    projections/gui/ColorPanel.java\
    projections/gui/ColorSelectWindow.java\
    projections/gui/ColorWindowFrame.java\
    projections/gui/Converter.java\
    projections/gui/FloatTextField.java\
    projections/gui/GraphAttributesWindow.java\
    projections/gui/GraphControlPanel.java\
    projections/gui/GraphData.java\
    projections/gui/GraphDisplayCanvas.java\
    projections/gui/GraphDisplayPanel.java\
    projections/gui/GraphIntervalDialog.java\
    projections/gui/GraphLegendPanel.java\
    projections/gui/GraphTitleCanvas.java\
    projections/gui/GraphWAxisCanvas.java\
    projections/gui/GraphWindow.java\
    projections/gui/GraphXAxisCanvas.java\
    projections/gui/GraphYAxisCanvas.java\
    projections/gui/GrayLWPanel.java\
    projections/gui/GrayPanel.java\
    projections/gui/HelpWindow.java\
    projections/gui/HistogramWindow.java\
    projections/gui/IntTextField.java\
    projections/gui/InvalidFileDialog.java\
    projections/gui/LWPanel.java\
    projections/gui/LogFileViewerDialog.java\
    projections/gui/LogFileViewerTextArea.java\
    projections/gui/LogFileViewerWindow.java\
    projections/gui/MainButtonPanel.java\
    projections/gui/MainTitlePanel.java\
    projections/gui/MainWindow.java\
    projections/gui/MyButton.java\
    projections/gui/OrderedGraphDataList.java\
    projections/gui/OrderedIntList.java\
    projections/gui/OrderedUsageList.java\
    projections/gui/ProfileAxisCanvas.java\
    projections/gui/ProfileColorWindow.java\
    projections/gui/ProfileData.java\
    projections/gui/ProfileDialog2.java\
    projections/gui/ProfileDisplayCanvas.java\
    projections/gui/ProfileLabelCanvas.java\
    projections/gui/ProfileLabelCanvas2.java\
    projections/gui/ProfileObject.java\
    projections/gui/ProfileTitleCanvas.java\
    projections/gui/ProfileWindow.java\
    projections/gui/ProfileYLabelCanvas.java\
    projections/gui/ScalePanel.java\
    projections/gui/ScaleSlider.java\
    projections/gui/SelectField.java\
    projections/gui/StlPanel.java\
    projections/gui/StlWindow.java\
    projections/gui/TimeTextField.java\
    projections/gui/TimelineAxisCanvas.java\
    projections/gui/TimelineColorWindow.java\
    projections/gui/TimelineData.java\
    projections/gui/TimelineDisplayCanvas.java\
    projections/gui/TimelineLabelCanvas.java\
    projections/gui/TimelineMessageCanvas.java\
    projections/gui/TimelineMessageWindow.java\
    projections/gui/TimelineObject.java\
    projections/gui/TimelineRangeDialog.java\
    projections/gui/TimelineWindow.java\
    projections/gui/U.java\
    projections/gui/Util.java\
    projections/gui/ZItem.java\
    projections/misc/EntryNotFoundException.java\
    projections/misc/LogLoadException.java\
    projections/misc/SummaryFormatException.java\
    projections/misc/ProgressDialog.java	\
    projections/misc/ErrorDialog.java


all: bin/projections.jar
	@ echo "Compilation complete!"  
	@ echo "See README or run bin/projections"

projections/gui/MainWindow.class: $(SRC)
	javac -d . -O $(SRC)

bin/projections.jar: projections/gui/MainWindow.class $(SRC)
	@ echo "** Creating jar file"
	jar -cfm0 bin/projections.jar \
		projections/images/manifest \
		projections/images/bgimage\
		projections/*/*.class

clean:
	@ echo "** Removing temporary files"
	- rm bin/*.jar
	- rm projections/*/*.class













