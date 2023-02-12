package com.vgrazi.pca;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.jgroups.ChannelException;

/**
 * <b>// Host side usage:</b>
 *
 * <pre>
 * private void startAppAnywhere() {
 *   try {
 *     AppAnywhereController appAnywhereController = new AppAnywhereController(jFrame);
 *     appAnywhereController.start();
 *   } catch (AWTException e) {
 *     e.printStackTrace();
 *   } catch (IOException e) {
 *     e.printStackTrace();
 *   } catch (ChannelException e) {
 *     e.printStackTrace();
 *   }
 * }
 * </pre>
 *
 * <b>client side usage: call</b><br>
 * java com.vgrazi.pca.AppLauncher client Created by Victor Grazi. Date: Sep 10,
 * 2006 - 9:12:55 PM
 */
public class AppLauncher {
  private static final Logger logger = Logger.getLogger(AppLauncher.class);
  private static final String clientFrameTitle = "VS7 Anywhere";
  private static AppAnywhereController controller;

  public static void main(String[] args) throws AWTException, IOException, ChannelException {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        controller.disconnect();
      }}));
    // args = new String[]{"localhost"};
    if (args.length == 0 || args[0].equalsIgnoreCase("server")) {
      launchServer();
    } else if (args[0].equalsIgnoreCase("both")) {
      launchServer();
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
      launchClient("localhost", clientFrameTitle);
    } else {
      launchClient(args[0], clientFrameTitle);
    }
  }

  private static void launchClient(String ipAddr, String clientFrameTitle) throws AWTException, IOException,
      ChannelException {
    controller = new AppAnywhereController(clientFrameTitle);
    controller.start();
  }

  private static void launchServer() throws AWTException, IOException, ChannelException {
    final JFrame appFrame = createTestAppFrame();
    controller = new AppAnywhereController(appFrame);
    controller.start();
  }

  private static void usage() {
    logger.info("Please call the following to launch the client");
    logger.info("java com.vgrazi.pca.AppLauncher <ip_addr>");
  }

  private static int imageIndex = 0;
  static String[] imageNames = { "images/Juan-Gris-1912.jpg", "images/PersistenceOfMemory.jpg",
      "images/MilletsArchitectural.jpg" };

  private static JFrame createTestAppFrame() {
    final JFrame appFrame = new JFrame("Test application");
    final JButton button = new JButton("Change imagePixels");
    final ImageIcon icon = getNextImageIcon();
    appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    final JLabel label = new JLabel(icon);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final ImageIcon newIcon = getNextImageIcon();
        final Image oldImage = icon.getImage();
        final Image image = newIcon.getImage();
        icon.setImage(image);
        if (oldImage != null) {
          oldImage.flush();
        }
        appFrame.pack();
      }
    });
    final JButton frameButton = new JButton("Launch dialog");
    frameButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showConfirmDialog(appFrame, "Testing");
      }
    });
    final JButton disconnectButton = new JButton("Disconnect");
    disconnectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        controller.disconnect();
      }
    });
    final JPanel panel = new JPanel(new FlowLayout());
    panel.add(button);
    panel.add(frameButton);
    panel.add(disconnectButton);
    appFrame.getContentPane().add(panel, BorderLayout.SOUTH);
    appFrame.getContentPane().add(label);
    appFrame.pack();
    appFrame.setVisible(true);
    return appFrame;
  }

  private static ImageIcon getNextImageIcon() {
    final String imageName = getNextImage();
    final ImageIcon newIcon = new ImageIcon(imageName);
    return newIcon;
  }

  private static String getNextImage() {
    if (imageIndex >= imageNames.length) {
      imageIndex = 0;
    }
    return imageNames[imageIndex++];
  }
}


/**
 *
 * $Log: AppLauncher.java,v $
 * Revision 1.13  2007/12/13 10:05:07  gmalik2
 * Adding shutdown hook to diconnect client
 *
 * Revision 1.12  2007/11/22 07:22:48  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */
