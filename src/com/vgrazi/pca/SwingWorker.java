package com.vgrazi.pca;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * This is the 3rd version of SwingWorker (also known as SwingWorker 3), an
 * abstract class that you subclass to perform GUI-related work in a dedicated
 * thread. For instructions on using this class, see: <a
 * href="http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html">http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html</a>
 * Note that the API changed slightly in the 3rd version: You must now invoke
 * start() on the SwingWorker after creating it.
 * <p>
 * Title: SwingWorker
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003
 * </p>
 * <p>
 * Company: CSFB HOLT
 * </p>
 *
 * @author (last mod by) $Author: gmalik2 $
 * @version $Revision: 1.3 $
 */

public abstract class SwingWorker {

  private static final Logger LOGGER = Logger.getLogger(SwingWorker.class);

  private Object value; // see getValue(), setValue()

  /**
   * Class to maintain reference to current worker thread under separate
   * synchronization control.
   */
  private static class ThreadVar {
    private Thread thread;

    ThreadVar(Thread t) {
      thread = t;
    }

    synchronized Thread get() {
      return thread;
    }

    synchronized void clear() {
      thread = null;
    }
  }

  private ThreadVar threadVar;

  /**
   * Get the value produced by the worker thread, or null if it hasn't been
   * constructed yet.
   */
  protected synchronized Object getValue() {
    return value;
  }

  /**
   * Set the value produced by worker thread
   */
  private synchronized void setValue(Object x) {
    value = x;
  }

  /**
   * Compute the value to be returned by the <code>get</code> method.
   */
  public abstract Object construct();

  /**
   * Called on the event dispatching thread (not on the worker thread) after the
   * <code>construct</code> method has returned.
   */
  public void finished() {
  }

  /**
   * A new method that interrupts the worker thread. Call this method to force
   * the worker to stop what it's doing.
   */
  public void interrupt() {
    Thread t = threadVar.get();
    if (t != null) {
      t.interrupt();
    }
    threadVar.clear();
  }

  /**
   * Return the value created by the <code>construct</code> method. Returns
   * null if either the constructing thread or the current thread was
   * interrupted before a value was produced.
   *
   * @return the value created by the <code>construct</code> method
   */
  public Object get() {
    while (true) {
      Thread t = threadVar.get();
      if (t == null) {
        return getValue();
      }
      try {
        t.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // propagate
        return null;
      }
    }
  }

  /**
   * Start a thread that will call the <code>construct</code> method and then
   * exit.
   */
  public SwingWorker() {
    final Runnable doFinished = new Runnable() {
      public void run() {
        try {
          finished();
        }
        // DAR - 5/21/04 - Added catch block to log exceptions
        catch (RuntimeException e) {
          LOGGER.error("Logging uncaught Exception", e);
          throw e;
        }
      }
    };

    Runnable doConstruct = new Runnable() {
      public void run() {
        try {
          setValue(construct());
        }
        // DAR - 5/21/04 - Added catch block to log exceptions
        catch (RuntimeException e) {
          LOGGER.error("Logging uncaught Exception", e);
          throw e;
        } finally {
          threadVar.clear();
        }

        SwingUtilities.invokeLater(doFinished);
      }
    };

    Thread t = new Thread(doConstruct);
    threadVar = new ThreadVar(t);
  }

  /**
   * Start the worker thread.
   */
  public void start() {
    Thread t = threadVar.get();
    if (t != null) {
      t.start();
    }
  }
} // ---------- end of class SwingWorker ----------

/**
 * $Log: SwingWorker.java,v $
 * Revision 1.3  2007/10/31 10:04:14  gmalik2
 * Refactoring
 * Revision 1.2 2006/11/06 22:23:50 vgrazi Removed
 * redundant import Revision 1.1 2006/11/06 21:59:26 vgrazi Initial revision
 * Revision 1.4 2005/04/27 18:58:35 vgrazi added href to javadoc Revision 1.3
 * 2004/05/24 15:42:44 drosenst (updated CVS revision tags)
 */

