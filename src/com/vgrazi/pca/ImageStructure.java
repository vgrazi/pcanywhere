package com.vgrazi.pca;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Victor Grazi. Date: Oct 1, 2006 - 2:35:33 AM
 */
public class ImageStructure implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Indicates that entire imagePixels is contained
   */
  public final static int COMPLETE_TYPE = 1;

  /**
   * Indicates that partial imagePixels is contained
   */
  public final static int DIFF_TYPE = 2;

  public int type;
  public final int width;
  public final int height;
  public int[] imagePixels;
  private byte[] zippedBytes;
  private Image image;
  private boolean disposed;
  private volatile static int nextSequence = 1;
  private final int sequence;

  public ImageStructure(int width, int height, int[] pixels, int imageType) {
    this(width, height, pixels, imageType, getNextSequence());
  }

  public ImageStructure(int width, int height, int[] pixels, int imageType, int sequence) {
    this.sequence = sequence;
    this.width = width;
    this.height = height;
    this.imagePixels = pixels;
    this.type = imageType;
  }

  public int getSequence() {
    return sequence;
  }

  public static synchronized int getNextSequence() {
    return nextSequence++;
  }

  public int[] getPixels() {
    return imagePixels;
  }

  public synchronized Image getImage() {
    if (image == null) {
      if (!disposed) {
        final ColorModel cm = ColorModel.getRGBdefault();
        image = Toolkit.getDefaultToolkit().createImage(
            new MemoryImageSource(width, height, cm, this.imagePixels, 0, width));
      }
    }
    return image;
  }

  public synchronized void dispose() {
    disposed = true;
    if (image != null) {
      image.flush();
      image = null;
    }
    imagePixels = null;
  }

  public String toString() {
    return "ImageStructure:" + sequence + " " + width + " x " + height + " " + getTypeDescription();
  }

  private String getTypeDescription() {
    return type == COMPLETE_TYPE ? "COMPLETE" : type == DIFF_TYPE ? " DIFFERENCES" : " UNKNOWN TYPE " + type;
  }

  public synchronized void compress() throws IOException {
    final int[] imagePixels = this.imagePixels;

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(bytesOut));
    out.writeObject(imagePixels);
    out.flush();
    out.close();
    this.zippedBytes = bytesOut.toByteArray();
    this.imagePixels = null;
  }

  public synchronized void decompress() throws IOException {
    final byte[] zippedBytes = this.zippedBytes;

    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(zippedBytes);
    final ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(bytesIn));
    try {
      this.imagePixels = (int[]) in.readObject();
      this.zippedBytes = null;
    } catch (final ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}


/**
 * $Log: ImageStructure.java,v $
 * Revision 1.3  2007/11/22 07:21:07  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 * Revision 1.2  10/31/2007 6:04 PM  gmalik2
 * Refactoring.
 */