# Makefile-ized "compile" script for
# projections-java.
# Converted by Orion Lawlor, 9/9/1999
#

SRC=\
    projections/analysis/AccumulatedSummaryReader.java\
    projections/analysis/AmpiFunctionData.java\
    projections/analysis/AsciiIntegerReader.java\
    projections/analysis/CallGraph.java\
    projections/analysis/CallTable.java\
    projections/analysis/CallStackManager.java\
    projections/analysis/Chare.java\
    projections/analysis/ColorSaver.java\
    projections/analysis/Entry.java\
    projections/analysis/EPNamdDefs.java\
    projections/analysis/EPDataGenerator.java\
    projections/analysis/IntervalData.java\
    projections/analysis/IntervalUtils.java\
    projections/analysis/LogEntry.java\
    projections/analysis/LogLoader.java\
    projections/analysis/LogReader.java\
    projections/analysis/PackTime.java\
    projections/analysis/PoseDopReader.java\
    projections/analysis/ProjDefs.java\
    projections/analysis/RangeHistory.java\
    projections/analysis/Sorter.java\
    projections/analysis/StsReader.java\
    projections/analysis/SumAnalyzer.java\
    projections/analysis/SumDetailReader.java\
    projections/analysis/SummaryFileReader.java\
    projections/analysis/TimelineEvent.java\
    projections/analysis/TimelineMessage.java\
    projections/analysis/UserEvent.java\
    projections/analysis/UsageCalc.java\
    projections/analysis/UsageInterval.java\
    projections/analysis/ViewerEvent.java\
    projections/analysis/MultiRunDataAnalyzer.java\
    projections/analysis/ParseTokenizer.java\
    projections/analysis/GenericLogReader.java\
    projections/analysis/GenericSummaryReader.java\
    projections/analysis/ObjectId.java\
    projections/analysis/ProjectionsFormatException.java\
    projections/analysis/ProjectionsConfigurationReader.java\
    projections/analysis/ProjectionsReader.java\
    projections/gui/AboutDialog.java\
    projections/gui/Analysis.java\
    projections/gui/AnimationColorBarPanel.java\
    projections/gui/AnimationDisplayPanel.java\
    projections/gui/AnimationWindow.java\
    projections/gui/BackGroundImagePanel.java\
    projections/gui/BinDialog.java\
    projections/gui/BItem.java\
    projections/gui/Bubble.java\
    projections/gui/CallTableTextArea.java\
    projections/gui/CallTableWindow.java\
    projections/gui/ColorMap.java\
    projections/gui/ColorPanel.java\
    projections/gui/ColorSelectable.java\
    projections/gui/ColorSelectWindow.java\
    projections/gui/ColorWindowFrame.java\
    projections/gui/CommTimeWindow.java\
    projections/gui/CommWindow.java\
    projections/gui/Converter.java\
    projections/gui/EntrySelectionDialog.java\
    projections/gui/EntryPointWindow.java\
    projections/gui/FormattedNumber.java\
    projections/gui/FloatTextField.java\
    projections/gui/FunctionTool.java\
    projections/gui/GenericGraphWindow.java\
    projections/gui/GraphAttributesWindow.java\
    projections/gui/GraphControlPanel.java\
    projections/gui/GraphData.java\
    projections/gui/GraphDisplayCanvas.java\
    projections/gui/GraphDisplayPanel.java\
    projections/gui/GraphLegendPanel.java\
    projections/gui/GraphTitleCanvas.java\
    projections/gui/GraphWAxisCanvas.java\
    projections/gui/GraphWindow.java\
    projections/gui/GraphXAxisCanvas.java\
    projections/gui/GraphYAxisCanvas.java\
    projections/gui/GrayLWPanel.java\
    projections/gui/GrayPanel.java\
    projections/gui/GrepFileFilter.java\
    projections/gui/HelpWindow.java\
    projections/gui/HistogramWindow.java\
    projections/gui/IntervalRangeDialog.java\
    projections/gui/IntervalWindow.java\
    projections/gui/IntTextField.java\
    projections/gui/InvalidFileDialog.java\
    projections/gui/JFloatTextField.java\
    projections/gui/JIntTextField.java\
    projections/gui/JLongTextField.java\
    projections/gui/JSelectField.java\
    projections/gui/JTimeTextField.java\
    projections/gui/LabelPanel.java\
    projections/gui/LWPanel.java\
    projections/gui/LogFileViewerDialog.java\
    projections/gui/LogFileViewerTextArea.java\
    projections/gui/LogFileViewerWindow.java\
    projections/gui/MainFileFilter.java\
    projections/gui/MainMenuManager.java\
    projections/gui/MainTitlePanel.java\
    projections/gui/MainWindow.java\
    projections/gui/MainRunStatusPanel.java\
    projections/gui/MainSummaryGraphPanel.java\
    projections/gui/MenuManager.java\
    projections/gui/MultiRunControlPanel.java\
    projections/gui/MultiRunFileDialogControl.java\
    projections/gui/MultiRunTables.java\
    projections/gui/MultiRunTextAreaWriter.java\
    projections/gui/MultiRunWindow.java\
    projections/gui/MyButton.java\
    projections/gui/OrderedGraphDataList.java\
    projections/gui/OrderedIntList.java\
    projections/gui/OrderedUsageList.java\
    projections/gui/PieChartWindow.java\
    projections/gui/PopUpAble.java\
    projections/gui/PoseAnalysisWindow.java\
    projections/gui/PoseRTDopDisplayPanel.java\
    projections/gui/PoseVTDopDisplayPanel.java\
    projections/gui/PoseRangeDialog.java\
    projections/gui/ProfileAxisCanvas.java\
    projections/gui/ProfileColorWindow.java\
    projections/gui/ProfileData.java\
    projections/gui/ProfileDisplayCanvas.java\
    projections/gui/ProfileLabelCanvas.java\
    projections/gui/ProfileLabelCanvas2.java\
    projections/gui/ProfileObject.java\
    projections/gui/ProfileTitleCanvas.java\
    projections/gui/ProfileWindow.java\
    projections/gui/ProfileYLabelCanvas.java\
    projections/gui/ProjectionsFileChooser.java\
    projections/gui/ProjectionsFileMgr.java\
    projections/gui/ProjectionsWindow.java\
    projections/gui/RangeDialog.java\
    projections/gui/Rubberband.java\
    projections/gui/RubberbandHorizontalZoom.java\
    projections/gui/ScalePanel.java\
    projections/gui/ScaleSlider.java\
    projections/gui/ScreenInfo.java\
    projections/gui/SelectField.java\
    projections/gui/StlPanel.java\
    projections/gui/StlWindow.java\
    projections/gui/SwingWorker.java\
    projections/gui/TimeProfileWindow.java\
    projections/gui/TimeTextField.java\
    projections/gui/TimelineAxisCanvas.java\
    projections/gui/TimelineColorWindow.java\
    projections/gui/TimelineData.java\
    projections/gui/TimelineDisplayCanvas.java\
    projections/gui/TimelineLabelCanvas.java\
    projections/gui/TimelineMessageCanvas.java\
    projections/gui/TimelineMessagePanel.java\
    projections/gui/TimelineMessageWindow.java\
    projections/gui/TimelineObject.java\
    projections/gui/TimelineWindow.java\
    projections/gui/TimelineLine.java\
    projections/gui/U.java\
    projections/gui/Util.java\
    projections/gui/UserEventWindow.java\
    projections/gui/Wait.java\
    projections/gui/WaitButton.java\
    projections/gui/ZItem.java\
    projections/gui/CallBack.java \
    projections/gui/count/TableMap.java\
    projections/gui/count/TableSorter.java\
    projections/misc/CommandLineException.java\
    projections/misc/EntryNotFoundException.java\
    projections/misc/ErrorDialog.java\
    projections/misc/LogEntryData.java\
    projections/misc/LogLoadException.java\
    projections/misc/MiscUtil.java\
    projections/misc/MultiRunData.java\
    projections/misc/MultiRunTableModel.java\
    projections/misc/PrintUtils.java\
    projections/misc/ProjectionsStatistics.java\
    projections/misc/SummaryFormatException.java\
    projections/gui/graph/DataSource.java\
    projections/gui/graph/DataSource1D.java\
    projections/gui/graph/DataSource2D.java\
    projections/gui/graph/GraphPanel.java\
    projections/gui/graph/Graph.java\
    projections/gui/graph/Coordinate.java\
    projections/gui/graph/LegendPanel.java\
    projections/gui/graph/LegendCanvas.java\
    projections/gui/graph/MultiRunDataSource.java\
    projections/gui/graph/MultiRunXAxis.java\
    projections/gui/graph/MultiRunYAxis.java\
    projections/gui/graph/SummaryDataSource.java\
    projections/gui/graph/SummaryXAxis.java\
    projections/gui/graph/SummaryYAxis.java\
    projections/gui/graph/XAxis.java\
    projections/gui/graph/XAxisDiscrete.java\
    projections/gui/graph/XAxisFixed.java\
    projections/gui/graph/YAxis.java\
    projections/gui/graph/YAxisFixed.java\
    projections/gui/graph/YAxisAuto.java


all: bin/projections.jar
	@ echo "Compilation complete!"  
	@ echo "See README or run bin/projections"

projections/gui/MainWindow.class: $(SRC)
	@ echo "Compiling java sources:"
	@ javac -sourcepath . -d . -deprecation -O $(SRC)
	@ echo "Complete."

bin/projections.jar: projections/gui/MainWindow.class $(SRC)
	@ echo "** Creating jar file"
	jar -cfm0 bin/projections.jar \
		projections/images/manifest \
		projections/images/bgimage\
		projections/*/*.class \
		projections/*/*/*.class

run: bin/projections.jar
	bin/projections test/hello.sts

clean:
	@ echo "** Removing temporary files"
	- rm -f bin/*.jar
	- rm -f projections/*/*.class
	- rm -f projections/*/*/*.class













