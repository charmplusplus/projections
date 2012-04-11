package projections.Tools.TopologyDisplay;

import java.io.File;

import javax.swing.filechooser.FileFilter;

class TopologyFileFilter extends FileFilter {
	public boolean accept(File file) {
		if ((file.getName().endsWith(".topo") && file.isFile()) ||
			 file.isDirectory()) {
			 return true;
		}
		return false;
	}

	public String getDescription() {
		return "*.topo";
	}
}

