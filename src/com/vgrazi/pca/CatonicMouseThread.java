package com.vgrazi.pca;

import java.awt.AWTEvent;
import java.awt.Event;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;

import org.apache.log4j.Logger;

/**
 * Created by Victor Grazi. Date: Sep 10, 2006 - 2:48:42 PM
 */
public class CatonicMouseThread {

  private static final Logger logger = Logger.getLogger(CatonicMouseThread.class);
  private final State state;
  private final Robot robot;

  public CatonicMouseThread(State state, Robot robot) {
    this.state = state;
    this.robot = robot;
    addListeners();
  }

  private void addListeners() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
      public void eventDispatched(AWTEvent awtEvent) {
        logger.debug("CatonicMouseThread.eventDispatched " + awtEvent);
        if (state.getControl() == State.PASSIVE_CONTROL) {
          final Point event = state.getLastMouseEvent();
          if (event != null) {
            final int x = event.x;
            final int y = event.y;
            logger.debug("CatonicMouseThread.eventDispatched moving mouse to " + x + "," + y);
            robot.mouseMove(x, y);
          }
        }
      }
    }, Event.MOUSE_MOVE);
  }
}




/**
 *
 * $Log: CatonicMouseThread.java,v $
 * Revision 1.3  2007/11/22 07:23:14  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */
