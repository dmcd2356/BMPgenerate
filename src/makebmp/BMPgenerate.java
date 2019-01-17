/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package makebmp;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Math.ceil;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;

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
  
  // constants
  private static final int MAX_WIDTH = 10;
  private static final int MAX_HEIGHT = 10;
  
  private static final int BYTES_PER_PIXEL = 3;  // bytes per pixel = 24 bits per pixel
  private static final int RES_PER_INCH = 72;    // resolution in pixels per inch

  private static final int BMP_HEADER_SIZE = 14; // size of Bitmap header
  private static final int DIB_HEADER_SIZE = 40; // size of DIB header
  private static final int COLOR_TBL_SIZE = 68;  // size of color table
  
  private static final double INCHES_PER_METER = 39.37;
  private static final int RES_PER_M = (int) ceil(RES_PER_INCH * INCHES_PER_METER);

  private final class BmpInfo {
    private byte[] fileContent;
    private int    fileSize;
    private int    bmpHeight;
    private int    bmpWidth;
    private int    bitsperpixel;
    private int    padbytes;
    
    public BmpInfo(int width, int height, int rgb) {
      // calculate the padding to add per row (each row must be in chunks of 4 bytes)
      padbytes = 4 - ((BYTES_PER_PIXEL * width) % 4);
      if (padbytes == 4) {
        padbytes = 0;
      }
    
      bitsperpixel = BYTES_PER_PIXEL * 8;
    
      // calculate size of file
      int imageoffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE + COLOR_TBL_SIZE;
      int imagesize = height * ((BYTES_PER_PIXEL * width) + padbytes);
      int filesize = imageoffset + imagesize;

      fileContent = new byte[filesize];
    
      int index = 0;
      // Bitmap file header
      index += putByte(index, 'B');
      index += putByte(index, 'M');
      index += put32bit(index, filesize); // size of file
      index += put16bit(index, 0); // reserved
      index += put16bit(index, 0); // reserved
      index += put32bit(index, imageoffset); // offset to pixel data

      // DIB header
      index += put32bit(index, DIB_HEADER_SIZE + COLOR_TBL_SIZE);
      index += put32bit(index, width);
      index += put32bit(index, height);
      index += put16bit(index, 1); // color planes (must be 1)
      index += put16bit(index, BYTES_PER_PIXEL * 8); // bits per pixel
      index += put32bit(index, 0); // compression method
      index += put32bit(index, imagesize); // image size in bytes
      index += put32bit(index, RES_PER_M); // horizontal resolution in pixels per meter
      index += put32bit(index, RES_PER_M); // vertical resolution in pixels per meter
      index += put32bit(index, 0); // number of colors in palette or 0 to default to 2^n
      index += put32bit(index, 0); // number of important colors used, or 0 if every color is important

      // Color table
      index += putByte(index, 'B');
      index += putByte(index, 'G');
      index += putByte(index, 'R');
      index += putByte(index, 's');
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 2);
      index += put32bit(index, 0);
      index += put32bit(index, 0);
      index += put32bit(index, 0);

      // image data
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          index += put24bit(index, rgb);
        }
        for (int pad = 0; pad < padbytes; pad++) {
          index += putByte(index, 0);
        }
      }
    
      bmpWidth = width;
      bmpHeight = height;
      printStatus("Image created.");
    }

    public BmpInfo(File file) {
      try {
        // verify file is bmp and get the height and width
        fileContent = Files.readAllBytes(file.toPath());
        if (fileContent.length < BMP_HEADER_SIZE || fileContent[0] != 'B' || fileContent[1] != 'M') {
          printStatus("File not a valid bitmap!");
          return;
        }
        fileSize = get32bit(2);
        int offset = get32bit(10);
        if (fileContent.length != fileSize || fileSize < offset) {
          printStatus("File not a valid bitmap - invalid size");
          fileSize = 0;
          return;
        }

        bmpWidth = get32bit(18);
        bmpHeight = get32bit(22);
        bitsperpixel = get16bit(28);

        if (bmpWidth > MAX_WIDTH || bmpHeight > MAX_HEIGHT) {
          printStatus("Bitmap is too large: " + bmpWidth + " x " + bmpHeight + " > 10 x 10");
          fileSize = 0;
          return;
        }
        
        if (bitsperpixel != BYTES_PER_PIXEL * 8) {
          printStatus("Bitmap is not a 24-bit per pixel type!");
          fileSize = 0;
          return;
        }
      
        // calculate the padding to add per row (each row must be in chunks of 4 bytes)
        padbytes = 4 - (bmpWidth % 4);
        if (padbytes == 4) {
          padbytes = 0;
        }
        printStatus("File loaded from: " + file.getAbsolutePath());
      } catch (IOException ex) {
        printStatus("Error reading file");
        System.err.println(ex.getMessage());
        fileSize = 0;
      }
    }
    
    public void saveToFile(String filename) {
      try (FileOutputStream fos = new FileOutputStream(filename)) {
        fos.write(fileContent);
        printStatus("File saved to: " + filename);
      } catch (IOException ex) {
        printStatus("Error writing file");
        System.err.println(ex.getMessage());
      }
    }
  
    public void setRGBEntry(int x, int y, int rgb) {
      // get file byte offset to start of image
      int imageoffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE + COLOR_TBL_SIZE;

      // get offset to the initial byte of selected row
      int rowwidth = BYTES_PER_PIXEL * bmpWidth + padbytes;
      int yindex = rowwidth * (bmpHeight - 1 - y);

      // get offset in row to 1st byte of pixel entry
      int xoffset = x * BYTES_PER_PIXEL;

      // set the intended value
      put24bit(imageoffset + yindex + xoffset, rgb);
    }
    
    public void displayBmpContents(HashMap<Integer, ArrayList<Integer>> changes) {
      // get file byte offset to start of image
      int imageoffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE + COLOR_TBL_SIZE;

      // get offset to the initial byte of 1st row
      int rowwidth = BYTES_PER_PIXEL * bmpWidth + padbytes;

      // allow editing
      imageLogger.getTextPanel().setEditable(true);
      imageLogger.clear();
      
      // display the data values
      int index = imageoffset + rowwidth * (bmpHeight - 1);
      for (int y = 0; y < bmpHeight; y++) {
        boolean changed = false;
        ArrayList<Integer> xlist = null;
        if (changes != null) {
          xlist = changes.get(y);
        }
        for (int x = 0; x < bmpWidth; x++) {
          if (xlist != null) {
            changed = xlist.contains(x);
          }
          // (rows are defined from bottom to top instead of top to bottom that user entered for Y)
          StringBuilder sb = new StringBuilder();
          for (int bix = 0; bix < BYTES_PER_PIXEL; bix++) {
            int xoffset = x * BYTES_PER_PIXEL;
            // bytes are read in reverse order on little-endian processors
            sb.append(String.format("%02X", fileContent[index + xoffset + (BYTES_PER_PIXEL - 1 - bix)]));
          }
          imageLogger.printRgbValue(changed, sb.toString() + "  ");
        }
        index -= rowwidth;
        imageLogger.printNewline();
      }

      // now disable editing
      imageLogger.getTextPanel().setEditable(false);
    }
    
    public int getByte(int index) {
      int entry = fileContent[index];
      return entry < 0 ? entry + 256 : entry;
    }
  
    public int get16bit(int index) {
      int value = 0;
      for (int ix = 1; ix >= 0; ix--) {
        value = (value << 8) + getByte(index + ix);
      }
      return value;
    }
  
    public int get32bit(int index) {
      int value = 0;
      for (int ix = 3; ix >= 0; ix--) {
        value = (value << 8) + getByte(index + ix);
      }
      return value;
    }
  
    public int putByte(int index, int value) {
      // assumes input value is always unsigned value from 0 to 255
      value = value < 0 ? 0 : value > 255 ? 0 : value;
      value = value > 127 ? value - 256 : value;
      fileContent[index] = (byte) value;
      return 1;
    }
  
    public int put16bit(int index, int value) {
      value = value < 0 ? 0 : value > 65535 ? 0 : value;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      return 2;
    }
  
    public int put24bit(int index, int value) {
      value = value < 0 ? 0 : value > 16777215 ? 0 : value;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      return 3;
    }
  
    public int put32bit(int index, int value) {
      value = value < 0 ? 0 : value;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      return 4;
    }
  
    public boolean isValid() {
      return fileSize > BMP_HEADER_SIZE;
    }

  }
  
  public void printStatus(String message) {
    mainFrame.getTextField("TXT_MESSAGES").setText(message);
  }
  
  public void createMainPanel() {
    // if a panel already exists, close the old one
    if (mainFrame.isValidFrame()) {
      mainFrame.close();
    }

    fileSelector = new JFileChooser();
    fileSelector.setCurrentDirectory(new File(System.getProperty("user.home")));
    
    GuiControls.Orient LEFT = GuiControls.Orient.LEFT;
    GuiControls.Orient NONE = GuiControls.Orient.NONE;
    
    // create the frame
    mainFrame.newFrame("BMPgenerate", 700, 600, GuiControls.FrameSize.FIXEDSIZE);

    String panel = null; // this creates the entries in the main frame
    mainFrame.makePanel(panel, "PNL_CONTROL" , LEFT, true , "Control Panel" , 680, 150);
    mainFrame.makePanel(panel, "PNL_INPUT"   , LEFT, true , "Data Input"    , 680, 150);
    mainFrame.makePanel(panel, "PNL_DISPLAY" , LEFT, true , "BMP pixel data", 680, 250);

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
          bmpImage.displayBmpContents(null);
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
          bmpImage.displayBmpContents(null);
          
          // update the size selections
          mainFrame.getSpinner("SPIN_WIDTH").getModel().setValue(bmpImage.bmpWidth);
          mainFrame.getSpinner("SPIN_HEIGHT").getModel().setValue(bmpImage.bmpHeight);
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
        bmpImage.displayBmpContents(null);
      }
    }
  }
  
  private class Action_ModifyBMP implements ActionListener {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {
      // this will keep track of the changes made (for highlighting)
      HashMap<Integer, ArrayList<Integer>> changes = new HashMap<>();
      
      int count = 0;
      String[] command = inputField.getText().split(FontInfo.NEWLINE);
      for (int ix = 0; ix < command.length; ix++) {
        String input = command[ix].trim();
        int start = input.indexOf("X_");
        int mid   = input.indexOf("_Y_");
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
            if (xval >= bmpImage.bmpWidth) {
              printStatus("entry " + ix + " X value of " + xval + " exceeded max width");
              break;
            }
            if (yval >= bmpImage.bmpHeight) {
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
            if (!changes.containsKey(yval)) {
              changes.put(yval, new ArrayList<>());
            }
            ArrayList<Integer> xset = changes.get(yval);
            if (xset.isEmpty() || !xset.contains(xval)) {
              xset.add(xval);
              count++;
            }
          } catch (NumberFormatException ex) {
            // indicate error
            break;
          }
        }
      }
      
      // now display updated values
      if (count > 0) {
        bmpImage.displayBmpContents(changes);
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
