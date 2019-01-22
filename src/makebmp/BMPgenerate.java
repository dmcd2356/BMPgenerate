/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package makebmp;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import static makebmp.BmpInfo.MAX_HEIGHT;
import static makebmp.BmpInfo.MAX_WIDTH;

/**
 *
 * @author dan
 */
public final class BMPgenerate {

  private static final GuiControls  mainFrame = new GuiControls();
  private static JFileChooser fileSelector;
  private static BmpInfo      bmpImage;
  private static JTextPane    inputField;
  private static ImageLogger  imageLogger;
  
  public static void printStatus(String message) {
    mainFrame.getTextField("TXT_MESSAGES").setText(message);
  }
  
  private void createMainPanel() {
    // if a panel already exists, close the old one
    if (mainFrame.isValidFrame()) {
      mainFrame.close();
    }

    fileSelector = new JFileChooser();
    fileSelector.setCurrentDirectory(new File(System.getProperty("user.home")));
    
    GuiControls.Orient LEFT = GuiControls.Orient.LEFT;
    GuiControls.Orient NONE = GuiControls.Orient.NONE;
    
    // create the frame
    mainFrame.newFrame("BMPgenerate", 720, 600, GuiControls.FrameSize.FIXEDSIZE);

    String panel = null; // this creates the entries in the main frame
    mainFrame.makePanel(panel, "PNL_CONTROL" , LEFT, true , "Control Panel" , 710, 150);
    mainFrame.makePanel(panel, "PNL_INPUT"   , LEFT, true , "Data Input"    , 710, 150);
    mainFrame.makePanel(panel, "PNL_DISPLAY" , LEFT, true , "BMP pixel data", 710, 260);

    panel = "PNL_CONTROL";
    JButton createButton = 
    mainFrame.makeButton    (panel, "BTN_CREATE"  , LEFT, false, "Create");
    mainFrame.makeLabel     (panel, "LBL_1"       , LEFT, false, "Width");
    mainFrame.makeSpinner   (panel, "SPIN_WIDTH"  , LEFT, false, 1, MAX_WIDTH, 1, 3);
    mainFrame.makeLabel     (panel, "LBL_2"       , LEFT, false, "Height");
    mainFrame.makeSpinner   (panel, "SPIN_HEIGHT" , LEFT, false, 1, MAX_HEIGHT, 1, 3);
    mainFrame.makeLabel     (panel, "LBL_3"       , LEFT, false, "RGB value");
    mainFrame.makeTextField (panel, "TXT_RGB"     , LEFT, true , "0", 9, true);

    JButton loadButton = 
    mainFrame.makeButton    (panel, "BTN_LOAD"    , LEFT, false, "Load");
    JButton saveButton = 
    mainFrame.makeButton    (panel, "BTN_SAVE"    , LEFT, true , "Save");
    mainFrame.makeLabel     (panel, "LBL_4"       , LEFT, true , "Status");
    mainFrame.makeTextField (panel, "TXT_MESSAGES", LEFT, true , "", 80, false);
    
    panel = "PNL_INPUT";
    JButton modifyButton = 
    mainFrame.makeButton    (panel, "BTN_MODIFY"  , LEFT, false, "Modify");
    inputField =
    mainFrame.makeScrollTextPane(panel, "TXT_INPUT");
    
    // create image logger for display
    panel = "PNL_DISPLAY";
    imageLogger = new ImageLogger("IMAGE");
    JScrollPane scrollPane = imageLogger.getScrollPanel();
    mainFrame.setGridBagLayout(panel, scrollPane, NONE, true, GuiControls.Expand.BOTH);
    mainFrame.addPanelToPanel(panel, scrollPane);

    // add action handlers
    createButton.addActionListener(new Action_CreateBMP());
    loadButton.addActionListener(new Action_LoadBMP());
    saveButton.addActionListener(new Action_SaveBMP());
    modifyButton.addActionListener(new Action_ModifyBMP());
        
    // activate the key & mouse listeners for the image logger
    imageLogger.activate();
    
    mainFrame.pack();
    mainFrame.display();
  }
  
  private class Action_CreateBMP implements ActionListener {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {
      Integer width  = (Integer) mainFrame.getSpinner("SPIN_WIDTH").getModel().getValue();
      Integer height = (Integer) mainFrame.getSpinner("SPIN_HEIGHT").getModel().getValue();
      String rgbstr = mainFrame.getTextField("TXT_RGB").getText();
      if (width != null && height != null && rgbstr != null) {
        try {
          int rgb;
          if (rgbstr.startsWith("x")) {
            rgb = Integer.parseUnsignedInt(rgbstr.substring(1), 16);
          } else {
            rgb = Integer.parseUnsignedInt(rgbstr);
          }
    
          // create the new file
          bmpImage = new BmpInfo(width, height, rgb);
      
          // now display it
          imageLogger.clearPixelMarks();
          imageLogger.displayRgbData(bmpImage);
        } catch (NumberFormatException ex) {
          // ignore if value was not numeric
        }
      }
    }
  }

  private class Action_LoadBMP implements ActionListener {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {
      String filetype = "bmp";
      FileNameExtensionFilter filter = new FileNameExtensionFilter(filetype.toUpperCase() + " Files", filetype);
      fileSelector.setFileFilter(filter);
      fileSelector.setSelectedFile(new File("image.bmp")); // default name selection
      fileSelector.setMultiSelectionEnabled(false);
      fileSelector.setApproveButtonText("Load");
      int retVal = fileSelector.showOpenDialog(mainFrame.getFrame());
      if (retVal == JFileChooser.APPROVE_OPTION) {
        // read the file
        bmpImage = new BmpInfo(fileSelector.getSelectedFile());
        
        // now display it
        if (bmpImage.isValid()) {
          imageLogger.clearPixelMarks();
          imageLogger.displayRgbData(bmpImage);
          
          // update the size selections
          mainFrame.getSpinner("SPIN_WIDTH").getModel().setValue(bmpImage.getWidth());
          mainFrame.getSpinner("SPIN_HEIGHT").getModel().setValue(bmpImage.getHeight());
        }
      }
    }
  }
  
  private class Action_SaveBMP implements ActionListener {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {
      String filetype = "bmp";
      FileNameExtensionFilter filter = new FileNameExtensionFilter(filetype.toUpperCase() + " Files", filetype);
      fileSelector.setFileFilter(filter);
      fileSelector.setSelectedFile(new File("image.bmp")); // default name selection
      fileSelector.setMultiSelectionEnabled(false);
      fileSelector.setApproveButtonText("Save");
      int retVal = fileSelector.showOpenDialog(mainFrame.getFrame());
      if (retVal == JFileChooser.APPROVE_OPTION) {
        // delete existing file
        File file = fileSelector.getSelectedFile();
        file.delete();
          
        // now write the bmp image to the file
        bmpImage.saveToFile(file.getAbsolutePath());
      
        // update display to clear any highlighting
        imageLogger.clearPixelMarks();
        imageLogger.displayRgbData(bmpImage);
      }
    }
  }
  
  private class Action_ModifyBMP implements ActionListener {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {
      // clear markings
      imageLogger.clearPixelMarks();
      
      int count = 0;
      String[] command = inputField.getText().split(FontInfo.NEWLINE);
      for (int ix = 0; ix < command.length; ix++) {
        String input = command[ix].trim();
        int start = input.toUpperCase().indexOf("X_");
        int mid   = input.toUpperCase().indexOf("_Y_");
        int end   = input.indexOf(" ");
        int valix = input.indexOf("= ");
        if (start >= 0 && mid > start && end > mid && valix > end) {
          try {
            String entry = input.substring(start + 2, mid);
            int xval = Integer.parseUnsignedInt(entry);
            entry = input.substring(mid + 3, end);
            int yval = Integer.parseUnsignedInt(entry);
            entry = input.substring(valix + 2);
            int rgb = Integer.parseUnsignedInt(entry);
            if (xval >= bmpImage.getWidth()) {
              printStatus("entry " + ix + " X value of " + xval + " exceeded max width");
              break;
            }
            if (yval >= bmpImage.getHeight()) {
              printStatus("entry " + ix + " Y value of " + yval + " exceeded max height");
              break;
            }
            if (rgb > 16777215) {
              printStatus("entry " + ix + " RGB value of " + rgb + " exceeded max range of 16777215");
              break;
            }
            
            // modify the entry
            bmpImage.setRGBEntry(xval, yval, rgb);
            
            // save the change coordinates
            count = imageLogger.markPixel(xval, yval);
          } catch (NumberFormatException ex) {
            // indicate error
            break;
          }
        }
      }
      
      // now display updated values
      if (count > 0) {
        imageLogger.displayRgbData(bmpImage);
        printStatus(command.length + " entries successfully changed");
      }
    }
  }
  
  public BMPgenerate() {
    createMainPanel();
  }
  
  public static void main(String[] args) throws IOException {
    // start the debug message panel
    BMPgenerate gui = new BMPgenerate();
  }

}
