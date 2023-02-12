package com.vgrazi.pca;

import java.awt.Point;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * Created by Victor Grazi. Date: Sep 10, 2006 - 10:02:38 AM
 */
public class State {

  private static final Logger logger = Logger.getLogger(State.class);
  public final static short ACTIVE_CONTROL = 0;
  public final static short PASSIVE_CONTROL = 1;
  private short control;

  public final static short SERVER_SIDE = 0;
  public final static short CLIENT_SIDE = 1;
  private short side;

  private Point lastMouseEvent;

  private final java.util.List stateChangeListeners = new ArrayList();
  public static final short CONTROL_CHANGE_EVENT = 1;
  public static final short SIDE_CHANGE_EVENT = 2;
  public static final short CONTROL_NOTIFICATION_EVENT = 3;
  private volatile String user = "Hosting server";
  private volatile String controllingUser = user;

  /**
   * Creates a new Client or Server side state
   *
   * @param side
   */
  public State(short side) {
    this.side = side;
    if (side == SERVER_SIDE) {
      control = ACTIVE_CONTROL;
    } else {
      control = PASSIVE_CONTROL;
    }
  }

  public boolean isServerSide() {
    return side == State.SERVER_SIDE;
  }

  public boolean isClientSide() {
    return side == State.CLIENT_SIDE;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public boolean isActive() {
    return control == State.ACTIVE_CONTROL;
  }

  public boolean isPassive() {
    return control == State.PASSIVE_CONTROL;
  }

  public ImageStructure getImageStructure() {
    return imageStructure;
  }

  public synchronized void setImageStructure(ImageStructure imageStructure) {
    final ImageStructure oldImageStructure = this.imageStructure;
    this.imageStructure = imageStructure;
    if (oldImageStructure != null) {
      oldImageStructure.dispose();
    }
    logger.debug("State.setImageStructure " + (oldImageStructure == null ? "Setting " : "Replacing ")
        + " new image structure " + imageStructure);
  }

  private ImageStructure imageStructure;

  /**
   * Returns either ACTIVE_CONTROL or PASSIVE_CONTROL depending on whether this
   * side or the other side has control.
   *
   * @return either ACTIVE_CONTROL or PASSIVE_CONTROL depending on whether this
   *         side or the other side has control.
   */
  public short getControl() {
    return control;
  }

  public void setControl(short control) {
    this.control = control;
  }

  /**
   * Returns this side SERVER_SIDE or CLIENT_SIDE
   *
   * @return this side SERVER_SIDE or CLIENT_SIDE
   */
  public short getSide() {
    return side;
  }

  public void setSide(short side) {
    this.side = side;
  }

  public Point getLastMouseEvent() {
    return lastMouseEvent;
  }

  public void setLastMouseEvent(Point lastMouseLocation) {
    this.lastMouseEvent = lastMouseLocation;
  }

  public void grabControl() {
    logger.debug("State.grabbing Control");
    setControl(ACTIVE_CONTROL);
    final Object oldControllingUser = this.controllingUser;
    controllingUser = user;
    notifyStateChangeListeners(CONTROL_CHANGE_EVENT, oldControllingUser, user);
  }

  public void forfeitControl(String newUser) {
    setControl(PASSIVE_CONTROL);
    final Object oldControllingUser = this.controllingUser;
    controllingUser = newUser;
    notifyStateChangeListeners(CONTROL_CHANGE_EVENT, oldControllingUser, newUser);
  }

  public void addStateChangeListener(StateChangeListener listener) {
    stateChangeListeners.add(listener);
  }

  public void removeStateChangeListener(StateChangeListener listener) {
    stateChangeListeners.remove(listener);
  }

  private void notifyStateChangeListeners(short eventType, Object oldState, Object newState) {
    for (int i = 0; i < stateChangeListeners.size(); i++) {
      final StateChangeListener listener = (StateChangeListener) stateChangeListeners.get(i);
      listener.stateChanged(eventType, oldState, newState);
    }
  }

  public void setImageStructure(int width, int height, int[] pixels, int imageType) {
    final ImageStructure oldImageStructure = imageStructure;
    imageStructure = new ImageStructure(width, height, pixels, imageType);
    final int[] newPixels = imageStructure.getPixels();
    boolean equal = false;
    if (oldImageStructure != null) {
      final int[] oldPixels = oldImageStructure.getPixels();
      equal = oldPixels.equals(newPixels);
    }
    if (oldImageStructure != null) {
      oldImageStructure.dispose();
    }
    logger.debug("State.setImageStructure old imagePixels " + (equal ? "EQUALS " : "NOT EQUAL TO") + "new imagePixels");
  }

  // todo: debug this
  public boolean applyIncomingImageStructure(ImageStructure incomingImageStructure) {
    boolean isApplied = false;
    final ImageStructure oldImageStructure = getImageStructure();
    int width = incomingImageStructure.width;
    int height = incomingImageStructure.height;
    int[] newImagePixels = incomingImageStructure.getPixels();
    if (incomingImageStructure.type == ImageStructure.COMPLETE_TYPE) {
      // Create an ImageStructure containing the complete image from the
      // incoming image structure
      isApplied = true;
    } else if ((oldImageStructure != null) && (oldImageStructure.imagePixels != null)) {
      // the other imagePixels is just the diff from ours. Apply it
      width = oldImageStructure.width;
      height = oldImageStructure.height;
      newImagePixels = new int[width * height];
      for (int i = 0; i < newImagePixels.length; i++) {
        newImagePixels[i] = incomingImageStructure.imagePixels[i] + oldImageStructure.imagePixels[i];
      }
      incomingImageStructure.imagePixels = newImagePixels;
      isApplied = true;
    }
    if (isApplied) {
      setImageStructure(width, height, newImagePixels, ImageStructure.COMPLETE_TYPE);
    }
    return isApplied;
  }

  public String toString() {
    final StringBuffer sb = new StringBuffer();

    sb.append(" User: ");
    sb.append(user);
    sb.append(" Side: ");
    sb.append(isClientSide() ? "Client" : "Server");
    sb.append(" Control: ");
    sb.append(isActive() ? "Controlling" : "Listening");

    return sb.toString();
  }

  public void setControllingUser(String user) {
    final Object oldControllingUser = this.controllingUser;
    controllingUser = user;
    notifyStateChangeListeners(CONTROL_NOTIFICATION_EVENT, oldControllingUser, user);
  }

  public String getControllingUser() {
    return controllingUser;
  }
}



/**
 *
 * $Log: State.java,v $
 * Revision 1.9  2007/11/22 07:24:18  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */
