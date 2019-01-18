/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package makebmp;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
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
  private static final HashMap<String, FontInfo> fontmap = new HashMap<>();
  
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
    // add key listener for the debug viewer
    panel.addKeyListener(new DebugKeyListener());
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

  public void printRgbValue(boolean changed, String value) {
    String type = changed ? MsgType.CHANGED.toString() : MsgType.NORMAL.toString();
    logger.printField(type, value);
  }
  
  public void printHeader(String header) {
    logger.printField(MsgType.HEADER.toString(), header);
  }
  
  public void printNewline() {
    logger.printLine("");
  }
  

  private class DebugKeyListener implements KeyListener {

    @Override
    public void keyPressed(KeyEvent ke) {
      // when the key is initially pressed
      //System.out.println("DebugKeyListener: keyPressed: " + ke.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent ke) {
      // follows keyPressed and preceeds keyReleased when entered key is character type
      //System.out.println("DebugKeyListener: keyTyped: " + ke.getKeyCode() + " = '" + ke.getKeyChar() + "'");
    }

    @Override
    public void keyReleased(KeyEvent ke) {
      // when the key has been released
      //System.out.println("DebugKeyListener: keyReleased: " + ke.getKeyCode());
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
