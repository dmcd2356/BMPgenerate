/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package makebmp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Math.ceil;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
  public final class BmpInfo {
    // constants
    public  static final int MAX_WIDTH = 10;
    public  static final int MAX_HEIGHT = 10;
  
    private static final int BYTES_PER_PIXEL = 3;  // bytes per pixel = 24 bits per pixel
    private static final int RES_PER_INCH = 72;    // resolution in pixels per inch

    private static final int BMP_HEADER_SIZE = 14; // size of Bitmap header
    private static final int DIB_HEADER_SIZE = 40; // size of DIB header
    private static final int COLOR_TBL_SIZE = 68;  // size of color table
  
    private static final double INCHES_PER_METER = 39.37;
    private static final int RES_PER_M = (int) ceil(RES_PER_INCH * INCHES_PER_METER);

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
      BMPgenerate.printStatus("Image created.");
    }

    public BmpInfo(File file) {
      try {
        // verify file is bmp and get the height and width
        fileContent = Files.readAllBytes(file.toPath());
        if (fileContent.length < BMP_HEADER_SIZE || fileContent[0] != 'B' || fileContent[1] != 'M') {
          BMPgenerate.printStatus("File not a valid bitmap!");
          return;
        }
        fileSize = get32bit(2);
        int offset = get32bit(10);
        if (fileContent.length != fileSize || fileSize < offset) {
          BMPgenerate.printStatus("File not a valid bitmap - invalid size");
          fileSize = 0;
          return;
        }

        bmpWidth = get32bit(18);
        bmpHeight = get32bit(22);
        bitsperpixel = get16bit(28);

        if (bmpWidth > MAX_WIDTH || bmpHeight > MAX_HEIGHT) {
          BMPgenerate.printStatus("Bitmap is too large: " + bmpWidth + " x " + bmpHeight + " > 10 x 10");
          fileSize = 0;
          return;
        }
        
        if (bitsperpixel != BYTES_PER_PIXEL * 8) {
          BMPgenerate.printStatus("Bitmap is not a 24-bit per pixel type!");
          fileSize = 0;
          return;
        }
      
        // calculate the padding to add per row (each row must be in chunks of 4 bytes)
        padbytes = 4 - (bmpWidth % 4);
        if (padbytes == 4) {
          padbytes = 0;
        }
        BMPgenerate.printStatus("File loaded from: " + file.getAbsolutePath());
      } catch (IOException ex) {
        BMPgenerate.printStatus("Error reading file");
        System.err.println(ex.getMessage());
        fileSize = 0;
      }
    }
    
    public boolean isValid() {
      return fileSize > BMP_HEADER_SIZE;
    }

    public int getHeight() {
      return bmpHeight;
    }
    
    public int getWidth() {
      return bmpWidth;
    }
    
    public void saveToFile(String filename) {
      try (FileOutputStream fos = new FileOutputStream(filename)) {
        fos.write(fileContent);
        BMPgenerate.printStatus("File saved to: " + filename);
      } catch (IOException ex) {
        BMPgenerate.printStatus("Error writing file");
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
    
    public int getRGBEntry(int x, int y) {
      // get file byte offset to start of image
      int imageoffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE + COLOR_TBL_SIZE;

      // get offset to the initial byte of selected row
      int rowwidth = BYTES_PER_PIXEL * bmpWidth + padbytes;
      int yindex = rowwidth * (bmpHeight - 1 - y);

      // get offset in row to 1st byte of pixel entry
      int xoffset = x * BYTES_PER_PIXEL;

      // get the pixel value
      return get24bit(imageoffset + yindex + xoffset);
    }
    
    public ArrayList<ArrayList<Integer>> getBmpContents() {
      // get file byte offset to start of image
      int imageoffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE + COLOR_TBL_SIZE;

      // get offset to the initial byte of 1st row
      int rowwidth = BYTES_PER_PIXEL * bmpWidth + padbytes;

      // get the offset to the start of the first row ( = last row of pixel map since
      // rows are defined from bottom to top instead of top to bottom as the image is displayed)
      int index = imageoffset + rowwidth * (bmpHeight - 1);

      // extract the RGB data values for each pixel and load into list
      ArrayList<ArrayList<Integer>> rowArray = new ArrayList<>();
      for (int y = 0; y < bmpHeight; y++) {
        ArrayList<Integer> colArray = new ArrayList<>();
        rowArray.add(colArray);
        
        for (int x = 0; x < bmpWidth; x++) {
          int rgb = get24bit(index + x * BYTES_PER_PIXEL);
          colArray.add(rgb);
        }
        index -= rowwidth;
      }

      return rowArray;
    }
    
    private int getByte(int index) {
      int entry = fileContent[index];
      return entry < 0 ? entry + 256 : entry;
    }
  
    private int get16bit(int index) {
      int value = 0;
      for (int ix = 1; ix >= 0; ix--) {
        value = (value << 8) + getByte(index + ix);
      }
      return value;
    }
  
    private int get24bit(int index) {
      int value = 0;
      for (int ix = 2; ix >= 0; ix--) {
        value = (value << 8) + getByte(index + ix);
      }
      return value;
    }
  
    private int get32bit(int index) {
      int value = 0;
      for (int ix = 3; ix >= 0; ix--) {
        value = (value << 8) + getByte(index + ix);
      }
      return value;
    }
  
    private int putByte(int index, int value) {
      // assumes input value is always unsigned value from 0 to 255
      value = value < 0 ? 0 : value > 255 ? 0 : value;
      value = value > 127 ? value - 256 : value;
      fileContent[index] = (byte) value;
      return 1;
    }
  
    private int put16bit(int index, int value) {
      value = value < 0 ? 0 : value > 65535 ? 0 : value;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      return 2;
    }
  
    private int put24bit(int index, int value) {
      value = value < 0 ? 0 : value > 16777215 ? 0 : value;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      value = value >> 8;
      fileContent[index++] = (byte) (value & 0xFF);
      return 3;
    }
  
    private int put32bit(int index, int value) {
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
  
  }
  
