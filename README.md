# Projections

![Projections CI](https://github.com/UIUC-PPL/projections/workflows/Projections%20CI/badge.svg?event=push)
[![Documentation Status](https://readthedocs.org/projects/charm/badge/?version=latest)](https://charm.readthedocs.io/en/latest/projections/manual.html)
[![DOI](https://zenodo.org/badge/86751036.svg)](https://zenodo.org/badge/latestdoi/86751036)


Projections is a tool to visusalize execution traces of Charm++ and AMPI applications.

## Compiling Projections

1) Make sure the JDK commands `java`, `javac` and `jar`
   are in your path. You should use at least Java version 8,
   and your java and javac should be of the same version.
   You also need [`gradle`](https://gradle.org/) and, optionally,
   the GNU tool `make`.

2) Type `make` (or `gradle copyJarToBin` if you don't have `make`).

3) The following files will be located in `bin`:

      `projections`          : Starts projections, for UNIX machines
      
      `projections.bat`       : Starts projections, for Windows machines
      
      `projections.jar`       : archive of all the java and image files


## Running Projections

Run `$ ./bin/projections` from the root of the Projections directory.

## Using Projections

Projections is a visualization tool to help you understand and analyze what it
happening in your parallel (Charm++) program.  To use Projections, you first
have to make sure that Charm++ is compiled with tracing enabled and that your
program is compiled with the Projections tracemode.  Usually, this means adding
`--enable-tracing` to your Charm++ build options and `-tracemode projections`
to your program compile options.  When you run your program compiled with this
tracemode, a log file will be created for each processor.  An additional state
file will also be created.  The names of the log files will be `NAME.#.log{.gz}`,
where NAME is the name of your executable and # is the processor #.  The name of
the state file is NAME.sts.

If your environment is set up correctly, as described in the previous section,
all you have to do to start Projections is type 'projections' at the prompt.

When Projections starts, you will see the main window with a toolbar with three
options, File, Tools, and Debug.

To begin using Projections, use "File -> Open File(s)" to select the data you
want to analyze.  To do so, navigate to the directory containing your data and
select the \*.sts file.  If you have selected a valid file, Projections will load
in some preliminary data from the files and then activate the rest of the buttons
in the main window.  If your file is invalid, you will be shown an error dialog.

After opening a file, there are many different tools you can use to analyze your
data:

1. Timelines
2. Usage Profile
3. Communication Per Processor
4. Communication Over Time
5. Call Table
6. View Log Files
7. Histograms
8. Overview
9. Animation
10. Time Profile
11. Performance Counters
12. User Events
13. User Stats Over Time
14. User Stats Per Processor
15. Extrema Analysis
16. Multirun Analysis
17. Noise Miner
18. Streaming CCS
19. Memory Usage

### Tool descriptions

#### 1. Timelines

The Timelines window lets you look at what a specific processor is doing at
each moment of the program.

When the Timelines window first appears, a dialog box appears along with it.
The box asks for the following information:

- Processors:  Choose which processor(s) you want to see a timeline for.
               To enter multiple values, separate them with a comma or a
               dash (for ranges).  Strides can be specified by
               using a colon to denote the stride size.  Some
               examples:

    | To see processors | Enter   |
    |-------------------|---------|
    | 1, 3, 5, 7        |  1,7:2  |
    | 1, 2, 3, 4        | 1-4     |
    | 1, 2, 3, 7        | 1,2,3,7 |



- Start Time  : Choose what time you want your timeline to start at.
- End Time    : Choose what time you want your timeline to end at.

The dialog box tells you what the valid processor choices are, as well as what
the valid time ranges are.

Particular time/processor ranges can be saved by using the "Add to
History List" button, which will save the currently selected
processors and time range for later use.  The dropdown menu above
the button shows the current saved ranges.  Particular ranges can
be removed by clicking the "Remove Selected History" button and the
history can be saved to disk for future runs using the "Save
History to Disk" button.

To select annotated timesteps, click the "Find Annotated Timesteps"
button. This finds user supplied notes on PE 0 that contain the
sigil "***".  The times that these notes were made on PE 0 are
annotated timesteps. After clicking the button, two dropdown
containing the names of the annotated timesteps are displayed, and
you can then select which timesteps you would like to use for the
start and end times of the range.

There are options to filter out entries shorter than a certain
time, filter out messages, filter out user events, and highlight
the top n longest idle and entry times.

To automatically select the portion of the execution with valid
performance data, click on "Adjust ranges to show useful
information" at the bottom of the window.

When you are satisfied with your time and processor ranges, click on 'OK'.
Projections will then get the Timeline data for you.  The time for this step
depends on the number of items in your time range and the number of
processors you have chosen.

The Timeline Window consists of two parts:

1) Display Panel:

   This is where the timelines are displayed and is the largest portion of
   the window.  The time axis is displayed at the top of the panel,
   and the units are microseconds.  The left side of the panel
   shows the processor labels.  Underneath each label are two numbers, the first
   indicating the percentage of the total time in your
   timeline that particular PE spent busy during the execution, and
   the second indicating the percentage of the total time that PE
   spent actually working on the application's entry methods.  The
   timeline itself consists of colored bars for each
   work item.  Placing the cursor over one of these bars will bring up a
   pop-up window telling you the name of that item, the begin time, the end
   time, and the total time.  It will also tell you what amount of time was
   spent packing, how many messages were created during this work item, and
   which processor created this item.  If you click on the item, by
   default, a line will be traced from the work item to the sender
   of the message responsible for this work item.  This behavior
   can be modified by selecting different options in the Tracing menu
   item.  Additionally, right clicking on a work item brings up a
   context menu, allowing you to view more details of that work
   item, change the color of that entry method, and drop or load
   other PEs based on their relation to that work item.  Viewing
   more details shows a window listing all of the messages
   created during this work item along with their targets and send
   times, along with other details of the work item.

2) Control Panel:

   - At the bottom of the window
   Checkboxes:
   - Display Pack Times
   - Display Message Sends
     These are represented by little white vertical lines at the
     time a message was created.
   - Display Idle Time
   - Display User Events
   - View n User Events
     Opens a new window showing user events on the selected PEs
   Buttons:
   - Load New Time/PE Range
     Brings up the initial dialog box to select a new range
   - Zoom In/Out
   - Zoom Selection / Load Selection
     After highlighting a section of the display by clicking and
     dragging in the time axis area, these options let you either zoom
     into the selected area or reload the selected area.


#### 2. Usage Profile:

The Usage Profile window lets you see percentage-wise what each processor
spends its time on during a specified period.

When the window first comes up, a dialog box appears asking for the
processor(s) you want to look at as well as the time range you want to look
at.  This is similar to the dialog for the Timelines.

The bottom portion of the Usage Profile window lets you adjust the scales in
both the X and Y directions.  The X direction is useful if you are looking at
a large number of processors.  The Y direction is useful if there are
small-percentage items for a processor.

The left side of the display shows a scale from 0% to 100%.  The main part of
the display shows the statistics.  Each processor is represented by a
vertical bar.  The top of the bar always shows the overhead time.  Below that
is always (if exists) the idle time and then the message packing/unpacking
times.  The rest of the bar is ordered from the bottom with the largest
percentage items being closest to the bottom.  If you place the cursor over a
portion of the bar, a pop-up window will appear telling you the name of the
item, what percent of the usage it has, and the processor it is on.


#### 3. Communication Per Processor:

This tool shows communication over the interval per selected
PE. This view is in the processor domain. It can show the number of
messages or bytes sent or received, depending on the option
selected at the bottom of the window.


#### 4. Communication Over Time:

This tool shows communication over time across all selected PEs.
This view is in the time domain. It can show the number of messages
or bytes sent or received, depending on the option selected at the
bottom of the window.


#### 5. Call Table:

This tool shows all the entry methods invoked by other entry
methods. The left aligned entry method name indicates the current
entry method, and the indented entry methods below are those called
by the current entry method.  Enabling EP Detail or Statistics at
the bottom shows the qualified name and parameter list of the EP
or the number of total messages received, bytes received, and
statistics on the received bytes, respectively.


#### 6. View Log Files:

This tool shows raw log files for the selected PEs and time
interval. The log files are parsed by Projections and event types,
entry method names, etc. are printed rather than the raw integers
of the actual logs.


#### 7. Histograms:

This tool creates histograms for various properties of the program,
execution time, accumulated execution time, message size, and idle
percentage.


#### 8. Overview:

This tool shows a high level overview of execution across the
selected PEs and time interval. It displays a dense, quantized view
of the execution of the program, either in terms of entry methods
or utilization.  Essentially, it provides a coarser view of the
same information as Timeline (see #1).


#### 9. Animations:

This window animates the processor usage by displaying
different colors for different amount of usage.

The left box allows you to select the real time between frames;
the right box the processor time between frames.


#### 10. Time Profile:

This tool shows execution across the selected PEs and time
interval. This view is in the time domain. This tool shows a high
level overview of execution across the selected PEs and time
interval. It displays a dense, quantized view of the execution of
the program, either in terms of entry methods or utilization.
Essentially, it provides a coarser view of the same information as
Timeline (see #1).


#### 11. Performance Counters:

This tool shows the values of performance counters per entry point
and PE. This option is disabled unless Charm++ was compiled with
support for PAPI counters.


#### 12. User Events:

This tool shows the summation of bracketed user events per PE
across the selected PEs and time interval.


#### 13. User Stats Over Time:

This tool shows the values of user stats over the program execution
across the selected PEs and time interval. This view is in the
time domain.


#### 14. User Stats Per Processor:

This tool shows the values of user stats per PE
across the selected PEs and time interval. This view is in the
PE domain.


#### 15. Extrema Analysis:

This tool identifies extreme clusters of particular attributes
through the execution. The attribute, activity, extrema threshold,
and number of clusters are customizable when selecting a time range.


#### 16. Multirun Analysis:

This tool compares multiple executions of a program to each other,
showing how time spent in entry methods changes across the
execution. Currently, this view only works with summary data due to
memory constraints.


#### 17. Noise Miner:

This tool identifies abnormally long entry methods to detect
symptoms consistent with computational noise.  Long events are
filtered and clustered to provide a summary of such occurrences.


#### 18. Streaming CCS:

This tool uses the Converse Client Server feature of Charm++ to
stream performance data from running programs.  To use it, the
Charm++ program must be compiled with `-tracemode utilization`, and
executed with `++server ++server-port <port number>`.


#### 19. Memory Usage:

This tool provides a view of memory utilization in the application
when it is linked with the memory tracing module.



## Notes

Charts in some tools are produced by JFreeChart.
Image output is performed by FreeHEP.
