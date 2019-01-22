/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package makebmp;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import static makebmp.FontInfo.TextColor.*;
import static makebmp.FontInfo.FontType.*;

/**
 *
 * @author dan
 */
public class ImageLogger {

  // types of messages
  private enum MsgType { NORMAL, CHANGED, HEADER }

  private static JTextPane       panel;
  private static JScrollPane     scrollPanel;
  private static Logger          logger;
  private static BmpInfo         bmpImage;
  private static final HashMap<String, FontInfo> fontmap = new HashMap<>();
  private static final HashMap<Integer, ArrayList<Integer>> changes = new HashMap<>();
  private static ArrayList<ArrayList<Integer>> rgbArray = new ArrayList<>();
  private static int count;
  
  public ImageLogger(String name) {
    String fonttype = "Courier";
    FontInfo.setTypeColor (fontmap, MsgType.HEADER.toString(),  Black,  Normal,     14, fonttype);
    FontInfo.setTypeColor (fontmap, MsgType.NORMAL.toString(),  Brown,  Bold,       14, fonttype);
    FontInfo.setTypeColor (fontmap, MsgType.CHANGED.toString(), Red,    BoldItalic, 14, fonttype);

    // create the text panel and assign it to the logger
    logger = new Logger(name, Logger.PanelType.TEXTPANE, true, fontmap);
    panel = (JTextPane) logger.getTextPanel();
    scrollPanel = logger.getScrollPanel();
  }
  
  public void activate() {
    // add mouse & key listeners for the bytecode viewer
    panel.addMouseListener(new PanelMouseListener());
    panel.addKeyListener(new PanelKeyListener());
  }
  
  public JTextPane getTextPanel() {
    return panel;
  }
  
  public JScrollPane getScrollPanel() {
    return scrollPanel;
  }
  
  public void clear() {
    logger.clear();
  }

  public void clearPixelMarks() {
    count = 0;
    changes.clear();
  }
  
  public int markPixel(int xval, int yval) {
    if (!changes.containsKey(yval)) {
      changes.put(yval, new ArrayList<>());
    }
    ArrayList<Integer> xset = changes.get(yval);
    if (xset.isEmpty() || !xset.contains(xval)) {
      xset.add(xval);
      count++;
    }
    return count;
  }
  
  /**
   * this displays the bmp image contents as rows and columns of RGB values in hexadecimal.
   * 
   * @param bmpInfo - the bmp image data
   */
  public void displayRgbData(BmpInfo bmpInfo) {
    // save the bmp image data
    bmpImage = bmpInfo;
    rgbArray = bmpImage.getBmpContents();
            
    // make panel writable clear the text
    getTextPanel().setEditable(true);
    clear();

    // display the column header
    String header = "   ";
    for (int icol = 0; icol < rgbArray.get(0).size(); icol++) {
      header += "  " + icol + "     ";
    }
    printHeader(header);
    printNewline();

    for (int irow = 0; irow < rgbArray.size(); irow++) {
      ArrayList<Integer> rgbRow = rgbArray.get(irow);
      ArrayList<Integer> xlist = (changes != null) ? changes.get(irow) : null;
      printHeader(irow + ": ");

      for (int icol = 0; icol < rgbRow.size(); icol++) {
        boolean changed = (xlist != null) ? xlist.contains(icol) : false;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%06X  ", rgbRow.get(icol)));
        printRgbValue(changed, sb.toString());
      }
      printNewline();
    }

    // set the panel to non-writable again
    printNewline();
    getTextPanel().setEditable(false);
  }
  
  private void printRgbValue(boolean changed, String value) {
    String type = changed ? MsgType.CHANGED.toString() : MsgType.NORMAL.toString();
    logger.printField(type, value);
  }
  
  private void printHeader(String header) {
    logger.printField(MsgType.HEADER.toString(), header);
  }
  
  private void printNewline() {
    logger.printLine("");
  }
  

  private class PanelMouseListener extends MouseAdapter {

    @Override
    public void mouseClicked (MouseEvent evt) {
      String contents = panel.getText();
      BMPgenerate.printStatus("");

      // set caret to the mouse location and get the caret position (char offset within text)
      panel.setCaretPosition(panel.viewToModel(evt.getPoint()));
      int curpos = panel.getCaretPosition();
      
      // now determine line number and offset within the line for the caret
      int row = -1; // to ignore the 1st line that is the header
      while (!contents.isEmpty()) {
        int offset = contents.indexOf('\n');
        if (offset < 0 || offset >= curpos) {
          break;
        }
        curpos -= offset + 1;
        contents = contents.substring(offset + 1);
        ++row;
      }
      curpos -= 4; // this accounts for the row header chars at the start of each line

      // skip if user selected header characters or the spaces between the columns
      if (row >= 0 && curpos >= 0 && curpos < 22 && curpos % 8 < 6) {
        int col = curpos / 8; // we occupy 8 chars per column, 6 of data and 2 spaces
        //BMPgenerate.printStatus("curpos = " + curpos + ", X = " + col + ", Y = " + row);
        
        // put up a panel to ask for new data value
        String s = (String)JOptionPane.showInputDialog(
                    null,
                    "Enter the new RGB value" + FontInfo.NEWLINE
                            + "(decimal assumed, beging value with 'x' for hexadecimal)",
                    "Modifying pixel (" + col + ", " + row + ")",
                    JOptionPane.PLAIN_MESSAGE,
                    null, // icon
                    null,
                    "ham");

        if ((s != null) && (s.length() > 0)) {
          // mark the entry to be modified
          markPixel(col, row);
          displayRgbData(bmpImage);
        }
      }
    }
  }
  
  private class PanelKeyListener implements KeyListener {

    @Override
    public void keyPressed(KeyEvent ke) {
      // when the key is initially pressed
      //System.out.println("PanelKeyListener: keyPressed: " + ke.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent ke) {
      // follows keyPressed and preceeds keyReleased when entered key is character type
      //System.out.println("PanelKeyListener: keyTyped: " + ke.getKeyCode() + " = '" + ke.getKeyChar() + "'");
    }

    @Override
    public void keyReleased(KeyEvent ke) {
      // when the key has been released
      //System.out.println("PanelKeyListener: keyReleased: " + ke.getKeyCode());
      //int curpos = panel.getCaretPosition();
      switch (ke.getKeyCode()) {
        case KeyEvent.VK_UP:
          break;
        case KeyEvent.VK_DOWN:
          break;
        case KeyEvent.VK_PAGE_UP:
          break;
        case KeyEvent.VK_PAGE_DOWN:
          break;
        default:
          break;
      }
    }
  }

}
