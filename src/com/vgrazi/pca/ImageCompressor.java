package com.vgrazi.pca;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class to convert Image into PNG/JPG If JPG compress it to specified quality.
 *
 * @author Gautam Malik (gmalik2)
 */
public class ImageCompressor {

  public static final String FORMAT_JPG = "JPG";
  public static final String FORMAT_PNG = "PNG";
  static {
    ImageIO.setUseCache(false);
  }

  /**
   * Read the input stream and convert it to Image
   *
   * @param in
   * @return
   * @throws IOException
   */
  private static BufferedImage read(InputStream in) throws IOException {
    final BufferedImage image = ImageIO.read(in);
    if (image == null) {
      throw new IOException("Read fails");
    }
    return image;
  }

  /**
   * Read the byte array and convert to Image
   *
   * @param bytes
   * @return
   */
  private static BufferedImage read(byte[] bytes) {
    try {
      return read(new ByteArrayInputStream(bytes));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convert into JPG. Compress the image to specified quality.
   *
   * @param image
   * @param quality
   * @param out
   * @throws IOException
   */
  private static void write(BufferedImage image, float quality, OutputStream out, String format) throws IOException {
    if (format != null) {
      final Iterator writers = ImageIO.getImageWritersByFormatName(format);
      if (!writers.hasNext()) {
        throw new IllegalStateException("No writers found");
      }
      final ImageWriter writer = (ImageWriter) writers.next();
      final ImageOutputStream ios = ImageIO.createImageOutputStream(out);
      writer.setOutput(ios);
      final ImageWriteParam param = writer.getDefaultWriteParam();
      // Not able to set the quality for png
      if (format.equalsIgnoreCase(FORMAT_JPG) && (quality >= 0)) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
      }
      writer.write(null, new IIOImage(image, null, null), param);
      ios.close();
      writer.dispose();
    }
  }

  /**
   * Convert the given image to byte array
   *
   * @param image
   * @param quality
   * @return
   */
  private static byte[] toByteArray(BufferedImage image, float quality, String format) {
    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream(50000);
      write(image, quality, out, format);
      return out.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts the image to PNG/JPG If JPG, comresses to specified quality.
   *
   * @param image
   * @param quality
   * @return
   */
  public static BufferedImage compress(BufferedImage image, float quality, String format) {
		return read(toByteArray(image, quality, format));
  }
}



/**
 *
 * $Log: ImageCompressor.java,v $
 * Revision 1.7  2007/11/22 07:23:55  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */