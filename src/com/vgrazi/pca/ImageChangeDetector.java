package com.vgrazi.pca;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

/**
 * This class takes care of detecting changes in the imagePixels, and posting
 * them to the client Created by Victor Grazi. Date: Sep 10, 2006 - 4:47:45 PM
 */
public class ImageChangeDetector {

  private static final Logger logger = Logger.getLogger(ImageChangeDetector.class);
  private final State state;
  private final Robot robot;
  /**
   * This is the parent JFrame to be captured.
   */
  private final JFrame parentFrame;
  private final AppAnywhereController controller;

  /**
   * @param state
   *          the State object containing state information, including the last
   *          imagePixels array
   * @param period
   *          time to wait in ms between imagePixels change checks
   * @param robot
   *          The Robot to use
   * @param frame
   *          The parent JFrame to capture
   * @param controller
   *          The controller
   */
  public ImageChangeDetector(State state, long period, Robot robot, JFrame frame, AppAnywhereController controller) {
    this.state = state;
    this.robot = robot;
    parentFrame = frame;
    this.controller = controller;
    launchThread(period);
    // launchTestFrame();
  }

  private void launchThread(long period) {
    final Timer timer = new Timer(true);
    timer.schedule(new TimerTask() {
      public void run() {
        // this is the only thread changing the imagePixels and dimensions in
        // the State object
        postImageDiffs();
      }
    }, 1000, period);
  }

  /**
   * Post images to client. DIFF or COMPLETE
   */
  private synchronized void postImageDiffs() {
    try {
      if (isWorking(parentFrame) && (parentFrame.getBounds().width > 0) && (parentFrame.getBounds().height > 0)) {
        // Grab the screenshot of window
        final ImageStructure newImageStructure = getImage();
        if (newImageStructure != null) {
          final int[] currentImage = newImageStructure.imagePixels;
          final int currentWidth = newImageStructure.width;
          final int currentHeight = newImageStructure.height;
          // Get the previous screenshot image
          final ImageStructure previousImageStructure = state.getImageStructure();
          int[] previousImage = null;
          int previousWidth = -1;
          int previousHeight = -1;
          if (previousImageStructure != null) {
            previousImage = previousImageStructure.imagePixels;
            previousWidth = previousImageStructure.width;
            previousHeight = previousImageStructure.height;
          }
          // System.setProperty(AppAnywhereConstants.TRANSMIT_DIFFS, "true");
          int[] diffs;
          boolean modified = false;
          int imageType;// diffs or complete
          if ((previousImage != null) && (currentWidth == previousWidth) && (currentHeight == previousHeight)) {
            // if dimensions haven't changed, diff the imagePixels. Else, pass
            // the whole imagePixels
            imageType = ImageStructure.DIFF_TYPE;
            diffs = new int[currentWidth * currentHeight];
            for (int i = 0; i < diffs.length; i++) {
              diffs[i] = currentImage[i] - previousImage[i];
              if (!modified && (diffs[i] != 0)) {
                modified = true;
              }
            }
            if (System.getProperty(AppAnywhereConstants.TRANSMIT_DIFFS, "false").equalsIgnoreCase("false")) {
              imageType = ImageStructure.COMPLETE_TYPE;
              diffs = currentImage;
            }
          } else {
            imageType = ImageStructure.COMPLETE_TYPE;
            diffs = currentImage;
            modified = true;
          }
          if (modified) {
            if (imageType == ImageStructure.DIFF_TYPE) {
              validateDiffs("DIFF", diffs);
            }
            state.setImageStructure(currentWidth, currentHeight, currentImage, ImageStructure.COMPLETE_TYPE);
            controller.postImageDiffs(currentWidth, currentHeight, diffs, imageType);
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private void validateDiffs(String label, int[] diffs) {
    for (int i = 1; i < diffs.length; i++) {
      final int diff = diffs[i];
      if (diff != diffs[0]) {
        logger.debug("ImageChangeDetector.validateDiffs DIFFERENCE FOUND IN POSITION " + i);
        return;
      }
    }
  }

  /**
   * This method will keep trying to post the Image when a new client connects
   * to the server.
   */
  synchronized void schedulePostImageDiffForced() {
    final Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      public void run() {
        postImageDiffForced(this);
      }
    }, 1000, 500);
  }

  /**
   * The screenshot is only taken once (when server window is focussed and
   * showing) After the screenshot is taken it is sent to the client.
   *
   * @param timerTask
   */
  private synchronized void postImageDiffForced(TimerTask timerTask) {
    if (isWorking(parentFrame) && (parentFrame.getBounds().width > 0) && (parentFrame.getBounds().height > 0)) {
      try {
        final ImageStructure imageStructure = getImage();
        if (imageStructure != null) {
          controller.postImageDiffs(imageStructure.width, imageStructure.height, imageStructure.imagePixels,
              ImageStructure.COMPLETE_TYPE);
          if (timerTask != null) {
            timerTask.cancel();
          }
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Takes the screenshot of the server window.
   *
   * @return ImageStructure
   */
  private synchronized ImageStructure getImage() {
    ImageStructure imageStructure = null;
    try {
      final Rectangle appBounds = new Rectangle(parentFrame.getBounds());
      appBounds.y += parentFrame.getInsets().top;
      appBounds.height -= parentFrame.getInsets().top + parentFrame.getInsets().bottom;
      appBounds.x += parentFrame.getInsets().left;
      appBounds.width -= parentFrame.getInsets().left + parentFrame.getInsets().right;
      final BufferedImage buffImage = robot.createScreenCapture(appBounds);
      final Image image = ImageCompressor.compress(buffImage, 0.1f, ImageCompressor.FORMAT_PNG);
      final int currentWidth = (int) appBounds.getWidth();
      final int currentHeight = (int) appBounds.getHeight();
      final int[] currentImage = new int[currentWidth * currentHeight];
      try {
        final PixelGrabber pg = new PixelGrabber(image, 0, 0, currentWidth, currentHeight, currentImage, 0, currentWidth);
        pg.grabPixels();
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
          throw new IOException("failed to load image contents");
        }
      } catch (final InterruptedException e) {
        throw new IOException("image load interrupted");
      }
      imageStructure = new ImageStructure(currentWidth, currentHeight, currentImage, ImageStructure.COMPLETE_TYPE);
      image.flush();
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return imageStructure;
  }

  /**
   * Test whether the frame or a subcomponent is focussed.
   *
   * @param frame
   * @return
   */
  public static boolean isWorking(JFrame frame) {
    if (frame != null) {
      if (frame.isFocused()) {
        return true;
      }
      final Window[] windows = frame.getOwnedWindows();
      for (final Window window : windows) {
        if (window.isFocused()) {
          return true;
        }
      }
    }
    return false;
  }
}




/**
 *
 * $Log: ImageChangeDetector.java,v $
 * Revision 1.17  2007/11/22 07:23:37  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */
