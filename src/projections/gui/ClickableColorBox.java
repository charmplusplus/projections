package projections.gui;

import java.awt.Color;

/// A class that incorporates an integer identifier and its corresponding paint
public abstract class ClickableColorBox {
		public int id;
		public Color c;
		public ClickableColorBox(int id, Color c){
			this.id = id;
			this.c = c;
		}
		public void setColor(Color c) {}
}
