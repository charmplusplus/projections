# Makefile-ized "compile" script for
# projections-java.
# Converted by Orion Lawlor, 9/9/1999
# Modified by Isaac Dooley to support multiple java versions 10/15/2007

# Type "make version" to see the version you are attempting to build
# we have two sets of source code we include depending on the version of
# javac we are using

# We include an unmodified version of jnt.FFT
# which is released under GPL, although much
# of the jnt.FFT code is not copyrighted


# The SRC used with jdk 1.3
SRC13=\
    projections/analysis/AccumulatedSummaryReader.java\
    projections/analysis/AmpiFunctionData.java\
    projections/analysis/AsciiIntegerReader.java\
    projections/analysis/CallGraph.java\
    projections/analysis/CallTable.java\
    projections/analysis/CallStackManager.java\
    projections/analysis/Chare.java\
    projections/analysis/Entry.java\
    projections/analysis/EPNamdDefs.java\
    projections/analysis/EPDataGenerator.java\
    projections/analysis/IntervalData.java\
    projections/analysis/IntervalUtils.java\
    projections/analysis/KMeansClustering.java\
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
    projections/analysis/TimelineEvent.java\
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
    projections/analysis/ProjMain.java\
    projections/gui/AboutDialog.java\
    projections/gui/Analysis.java\
    projections/gui/AmpiTimeProfileWindow.java\
    projections/gui/AnimationColorBarPanel.java\
    projections/gui/AnimationDisplayPanel.java\
    projections/gui/AnimationWindow.java\
    projections/gui/BackGroundImagePanel.java\
    projections/gui/BinDialog.java\
    projections/gui/BItem.java\
    projections/gui/Bubble.java\
    projections/gui/CallTableTextArea.java\
    projections/gui/CallTableWindow.java\
    projections/gui/Clickable.java\
    projections/gui/ColorManager.java\
    projections/gui/ColorMap.java\
    projections/gui/ColorPanel.java\
    projections/gui/ColorSelectable.java\
    projections/gui/ColorSelectWindow.java\
    projections/gui/ColorWindowFrame.java\
    projections/gui/CommTimeWindow.java\
    projections/gui/CommWindow.java\
    projections/gui/Converter.java\
    projections/gui/DialogParameters.java\
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
    projections/gui/OutlierAnalysisWindow.java\
    projections/gui/OutlierDialog.java\
    projections/gui/PieChartWindow.java\
    projections/gui/PopUpAble.java\
    projections/gui/PoseAnalysisWindow.java\
    projections/gui/PoseRTDopDisplayPanel.java\
    projections/gui/PoseVTDopDisplayPanel.java\
    projections/gui/PoseRangeDialog.java\
    projections/gui/ProfileAxisCanvas.java\
    projections/gui/ProfileColorWindow.java\
    projections/gui/ProfileData.java\
    projections/gui/ProfileLabelCanvas.java\
    projections/gui/ProfileLabelCanvas2.java\
    projections/gui/ProfileObject.java\
    projections/gui/ProfileTitleCanvas.java\
    projections/gui/ProfileWindow.java\
    projections/gui/AmpiProfileData.java\
    projections/gui/AmpiProfileWindow.java\
    projections/gui/ProfileYLabelCanvas.java\
    projections/gui/ProjectionsFileChooser.java\
    projections/gui/ProjectionsFileMgr.java\
    projections/gui/ProjectionsWindow.java\
    projections/gui/RangeDialog.java\
    projections/gui/ResponsiveToMouse.java\
    projections/gui/ScalePanel.java\
    projections/gui/ScaleSlider.java\
    projections/gui/ScreenInfo.java\
    projections/gui/SelectField.java\
    projections/gui/StlPanel.java\
    projections/gui/StlWindow.java\
    projections/gui/SwingWorker.java\
    projections/gui/TimeProfileWindow.java\
    projections/gui/TimeTextField.java\
	projections/gui/Timeline/AxisPanel.java \
    projections/gui/Timeline/ColorChooser.java \
    projections/gui/Timeline/Data.java \
    projections/gui/Timeline/LabelPanel.java \
    projections/gui/Timeline/MainPanel.java \
    projections/gui/Timeline/MainHandler.java \
    projections/gui/Timeline/MessageCanvas.java \
    projections/gui/Timeline/MessagePanel.java \
    projections/gui/Timeline/MessageWindow.java \
    projections/gui/Timeline/EntryMethodObject.java \
    projections/gui/Timeline/SaveImage.java \
    projections/gui/Timeline/NonScrollingPanel.java \
    projections/gui/Timeline/NonScrollingLayout.java \
    projections/gui/Timeline/ScrollingPanel.java \
	projections/gui/Timeline/TimelineScrollPaneLayout.java \
    projections/gui/Timeline/UserEventObject.java \
    projections/gui/Timeline/UserEventsWindow.java\
    projections/gui/Timeline/UserEventWindow.java\
    projections/gui/Timeline/WindowControls.java \
    projections/gui/Timeline/TimelineWindow.java \
    projections/gui/U.java\
    projections/gui/Util.java\
    projections/gui/Wait.java\
    projections/gui/WaitButton.java\
    projections/gui/ZItem.java\
    projections/gui/CallBack.java \
    projections/gui/count/TableMap.java\
    projections/gui/count/TableSorter.java\
    projections/misc/CommandLineException.java\
    projections/misc/EntryNotFoundException.java\
    projections/misc/ErrorDialog.java\
    projections/misc/FileUtils.java\
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
    projections/gui/graph/XAxisDiscreteOrdered.java\
    projections/gui/graph/XAxisFixed.java\
    projections/gui/graph/YAxis.java\
    projections/gui/graph/YAxisFixed.java\
    projections/gui/graph/YAxisAuto.java\


# The SRC used with jdk 1.5 or newer
# This includes the NoiseMiner tool which only works with JDK 1.5 or higher because it uses generics/templates
SRC15=$(SRC13) \
    projections/analysis/NoiseMiner.java\
    projections/gui/NoiseMinerWindow.java\
	projections/gui/NoiseMinerExemplarTimelineWindow.java\
	jnt/FFT/ComplexDouble2DFFT.java\
	jnt/FFT/ComplexDoubleFFT.java\
	jnt/FFT/ComplexDoubleFFT_Mixed.java\
	jnt/FFT/ComplexDoubleFFT_Radix2.java\
	jnt/FFT/ComplexFloat2DFFT.java\
	jnt/FFT/ComplexFloatFFT.java\
	jnt/FFT/ComplexFloatFFT_Mixed.java\
	jnt/FFT/ComplexFloatFFT_Radix2.java\
	jnt/FFT/Factorize.java\
	jnt/FFT/RealDoubleFFT.java\
	jnt/FFT/RealDoubleFFT_Even.java\
	jnt/FFT/RealDoubleFFT_Radix2.java\
	jnt/FFT/RealFloat2DFFT_Even.java\
	jnt/FFT/RealFloatFFT.java\
	jnt/FFT/RealFloatFFT_Radix2.java


#determine the version of the java compiler we are using
# JVERSION will contain something like "1.5" or "1.3"
JVERSION :=$(shell javac -version 2>&1 | sed -n 's/javac \([0-9]\)\.\([0-9]\)\.\([0-9]\).*/\1\.\2/p')

# Chose the appropriate list of valid source files based on the java version
ifeq "$(JVERSION)" "1.5"
SRC=$(SRC15)
JVDESC="Java 1.5"
else 
ifeq "$(JVERSION)" "1.6"
SRC=$(SRC15)
JVDESC="Java 1.5"
else 
ifeq "$(JVERSION)" "1.7"
SRC=$(SRC15)
JVDESC="Java 1.5"
else
SRC=$(SRC13)
JVDESC="Java 1.3"
endif
endif
endif


# And now for the real rules:

all: bin/projections.jar
	@ echo "Compilation complete!"  
	@ echo "See README or run bin/projections"

projections/analysis/ProjMain.class: $(SRC)
	@ echo "Compiling java sources:"
	@ javac -sourcepath . -d . -deprecation -O $(SRC)
	@ echo "Complete."

bin/projections.jar: projections/analysis/ProjMain.class $(SRC)
	@ echo "** Creating jar file"
	jar -cfm0 bin/projections.jar \
		projections/images/manifest \
		projections/images/bgimage\
		*/*/*.class \
		*/*/*/*.class 

run: bin/projections.jar
	bin/projections test/hello.sts

version:
	@ echo "Compiling code associated with $(JVDESC)"

clean:
	@ echo "** Removing temporary files"
	- rm -f bin/*.jar
	- rm -f projections/*/*.class
	- rm -f projections/*/*/*.class
	- rm -f jnt/FFT/*.class
