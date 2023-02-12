package com.vgrazi.pca;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;

/**
 * Created by Victor Grazi. Date: Sep 9, 2006 - 11:43:58 PM
 */
public class AppAnywhereController {
  private static final Logger logger = Logger.getLogger(AppAnywhereController.class);
  private static View myView = null;
  private final Robot robot;
  private final List OUTGOING_QUEUE = new ArrayList();
  private final List INCOMING_QUEUE = new ArrayList();
  private final State state;
  AWTEventListener awtEventListener;

  private boolean running = true;
  JChannel channel;
  private volatile boolean showing = true;

  // the amount of time in ms to sleep between applying incoming mouse events
  private long INCOMING_MOUSE_DELAY = 0;
  private boolean mouseDisabled;

  private Thread transmitThread;

  private JFrame clientFrame;

  private JFrame serverFrame;
  private final ImageIcon imageIcon = new ImageIcon();
  private JLabel clientLabel;
  private short side;
  /**
   * Used to save the server title, to restore when complete
   */
  private String serverFrameTitle;
  private final String clientFrameTitle;
  private Dimension clientFrameSize;
  private JPanel loginPanel = new JPanel(null);

  ImageChangeDetector imageChangeDetector;

  /**
   * Launch the controller on the client side
   *
   * @throws java.awt.AWTException
   */
  public AppAnywhereController(String clientFrameTitle) throws AWTException, IOException, ChannelException {
    this(null, clientFrameTitle);
  }

  /**
   * Launch the controller on the server side
   *
   * @throws java.awt.AWTException
   */
  public AppAnywhereController(JFrame frame) throws AWTException, IOException, ChannelException {
    this(frame, null);
  }

  /**
   * Many entry point indirectly used by client and server apps
   *
   * @param serverFrame
   *          the JFrame which will be transmitted on the server side. If this
   *          is a server instance send null.
   * @param clientFrameTitle
   *          the name of the frame to use on the client side, if this is a
   *          client side instance. If this is a client instance send null
   * @throws java.awt.AWTException
   */
  private AppAnywhereController(JFrame serverFrame, String clientFrameTitle) throws AWTException, IOException,
      ChannelException {
    logger.info("AppAnywhereController.AppAnywhereController $Revision: 1.43 $");
    this.clientFrameTitle = clientFrameTitle;
    this.serverFrame = serverFrame;
    if (serverFrame != null) {
      serverFrameTitle = serverFrame.getTitle();
      serverFrame.setFocusable(true);
      serverFrame.addWindowFocusListener(new WindowFocusListener() {
        public void windowGainedFocus(WindowEvent e) {
          setShowing(true);
        }

        public void windowLostFocus(WindowEvent e) {
          if (e.getOppositeWindow() != null) {
            setShowing(true);
          } else {
            setShowing(false);
          }
        }
      });
    }
    side = serverFrame != null ? State.SERVER_SIDE : State.CLIENT_SIDE;
    state = new State(side);
    mouseDisabled = System.getProperty(AppAnywhereConstants.MOUSE_DISABLED, "false").equalsIgnoreCase("true");
    logger.info("AppAnywhereContoller.AppAnywhereContoller mouse " + (mouseDisabled ? "DIS" : "EN") + "abled");

    if (System.getProperty(AppAnywhereConstants.STATS_FRAME, "false").equalsIgnoreCase("true")) {
      setupStatsFrame();
    }

    robot = new Robot();
  }

  public void start() throws ChannelException, IOException {
    if (side == State.SERVER_SIDE) {
      connect();
      imageChangeDetector = new ImageChangeDetector(state, 1000, robot, serverFrame, AppAnywhereController.this);
    } else {
      launchClientFrame(clientFrameTitle);
    }
    addStateChangeListener(getFrame());
  }

  /**
   * Creates the proper system property from the supplied parameters, then
   * connects. The generic connect method reads that system property to
   * establish the connection
   *
   * @param loginName
   * @param hostName
   * @param portName
   */
  private void connect(String loginName, String hostName, String portName) {
    try {
      state.setUser(loginName);
      StringBuffer property = new StringBuffer();
      property.append(hostName);
      property.append('[');
      property.append(portName);
      property.append(']');
      System.setProperty(AppAnywhereConstants.JGROUPS_HOST, property.toString());
      logger.info("property:" + property);
      connect();
      postCommand(new Command(Command.REQUEST_CONTROLLER_ID));
      postCommand(new Command(Command.CLIENT_RESPOST_IMAGE));
    } catch (ChannelException e1) {
      e1.printStackTrace();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Sets up the channels and adds the event listeners and other threads
   *
   * @throws ChannelException
   */
  private void connect() throws ChannelException, IOException {
    channel = new JChannel(AppAnywhereConstants.props);
    channel.setReceiver(new ExtendedReceiverAdapter() {
      public void receive(Message msg) {
        try {
          processingIncomingObject(msg.getObject());
        } catch (IOException e) {
          e.printStackTrace();
        }
        /*
         * synchronized(INCOMING_QUEUE) { INCOMING_QUEUE.add(msg.getObject());
         * INCOMING_QUEUE.notify(); }
         */
      }

      public void viewAccepted(View view) {
        processView(view);
      }
    });
    // exclude self from receiving
    channel.setOpt(Channel.LOCAL, Boolean.FALSE);
    // channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
    channel.connect("HOLT");
    // Flow - on app-side, imagePixels thread posts imagePixels changes to queue
    // On both side,
    // If passive, Toolkit listener posts only Mouse double click Events to the
    // queue.
    // If active, Toolkit listener posts MouseEvents to the queue.
    // Structures:
    // Queue processing thread-processes imagePixels and mouse commands on
    // incoming queue
    // Image change detector - On app side only, detects imagePixels changes and
    // pushes them to queue
    // mouse move prevention thread - on passive side only - prevents mouse from
    // moving
    // State guard - maintains current state (application/client,
    // active/passive) - All mouse motions and
    // imagePixels change detections ask the state guard before posting to the
    // command queue

    if (!mouseDisabled) {
      addEventListener();
    }

    startThreads(serverFrame);
  }

  private void processView(View newView) {
    Vector newMember = newView.getMembers();
    Vector oldMember;
    if (myView == null) {
      oldMember = new Vector();
    } else {
      oldMember = myView.getMembers();
    }
    List joinList = getNewMember(newMember, oldMember);
    List leaveList = getNewMember(oldMember, newMember);
    if (joinList.size() > 0) {
      logger.info("The member " + joinList + " joined!");
    }
    if (leaveList.size() > 0) {
      logger.info("The member " + leaveList + " left!");
    }
    myView = newView;
  }

  private List getNewMember(Vector newMembers, Vector oldMembers) {
    List list = new ArrayList();
    for (int i = 0; i < newMembers.size(); i++) {
      Object m = newMembers.get(i);
      if (!oldMembers.contains(m)) {
        list.add(m);
      }
    }
    return list;
  }

  final int MAX_SIZE = 3;
  final JLabel statsLabel = new JLabel();

  private void setupStatsFrame() {
    JFrame statsFrame = new JFrame();
    statsLabel.setVerticalAlignment(JLabel.TOP);
    statsLabel.setPreferredSize(new Dimension(300, 400));
    statsFrame.getContentPane().add(statsLabel);
    JButton button = new JButton("GC");
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.gc();
        // updateStatsFrame();
      }
    });
    statsFrame.getContentPane().add(button, BorderLayout.NORTH);
    // updateStatsFrame();
    statsFrame.pack();
    statsFrame.setLocation(500, 600);
    statsFrame.setVisible(true);
  }

  private void updateStatsFrame() {
    updateStatsFrame(null);
  }

  private void updateStatsFrame(String text) {
    NumberFormat format = new DecimalFormat("#,##0");
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < OUTGOING_QUEUE.size() && i < MAX_SIZE; i++) {
      Object stackObject = OUTGOING_QUEUE.get(i);
      sb.append("<br>  ");
      sb.append(stackObject);
    }
    if (OUTGOING_QUEUE.size() > MAX_SIZE) {
      sb.append("<br>  ....");
    }
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    long max = Runtime.getRuntime().maxMemory();
    long used = total - free;
    statsLabel.setText("<html>" + "Statistics:" + "<br>Used  memory:" + format.format(used / 1000000) + " MB"
        + "<br>Max   memory:" + format.format(max / 1000000) + " MB" + "<br>Total memory:"
        + format.format(total / 1000000) + " MB" + "<br>Free  memory:" + format.format(free / 1000000) + " MB"
        + "<br>OUTGOING STACK:" + OUTGOING_QUEUE.size() + sb + (text != null ? "<br>" + text : "") + "</html>");
  }

  private void launchClientFrame(String clientFrameTitle) throws ChannelException {
    clientFrame = new JFrame(clientFrameTitle);
    clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    clientFrame.addWindowListener(new WindowAdapter() {

      public void windowClosing(WindowEvent e) {
        clientFrame.setVisible(false);
        disconnect();
      }
    });
    ImageIcon imageIcon = new ImageIcon(AppAnywhereConstants.IMAGE_URL);
    final JLabel imageLabel = new JLabel(imageIcon);
    Insets insets = clientFrame.getInsets();
    Dimension preferredSize = imageLabel.getPreferredSize();
    clientFrameSize = new Dimension(preferredSize.width + insets.left + insets.right + 10, preferredSize.height
        + insets.top + insets.bottom + 25);
    imageLabel.setBounds(insets.left, insets.top, preferredSize.width, preferredSize.height);
    // final JLabel textLabel = new JLabel(AppAnywhereConstants.INIT_TEXT);
    logger.debug("AppAnywhereController.launchClientFrame HTML:" + AppAnywhereConstants.INIT_TEXT);
    final JButton connectButton = new JButton("Connect");
    Dimension buttonPreferredSize = connectButton.getPreferredSize();
    int buttonXPos = 50;
    int buttonYPos = preferredSize.height - buttonPreferredSize.height - 50;
    connectButton.setBounds(buttonXPos, buttonYPos, buttonPreferredSize.width, buttonPreferredSize.height);
    final JLabel loginLabel = new JLabel("Name");
    loginLabel.setForeground(Color.white);
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int loginLabelXPos = 10;
    int loginLabelYPos = buttonYPos - 30;
    loginLabel.setBounds(loginLabelXPos, loginLabelYPos, 100, 20);

    final JTextField login = new JTextField("client");
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int loginXPos = buttonXPos;
    int loginYPos = loginLabelYPos;
    login.setBounds(loginXPos, loginYPos, 100, 20);

    final JLabel hostLabel = new JLabel("Host");
    hostLabel.setForeground(Color.white);
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int hostLabelXPos = 10;
    int hostLabelYPos = loginLabelYPos - 30;
    hostLabel.setBounds(hostLabelXPos, hostLabelYPos, 100, 20);

    final JTextField host = new JTextField("localhost");
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int hostXPos = buttonXPos;
    int hostYPos = loginLabelYPos - 30;
    host.setBounds(hostXPos, hostYPos, 100, 20);

    final JLabel portLabel = new JLabel("Port");
    portLabel.setForeground(Color.white);
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int portLabelXPos = hostLabelXPos + 180;
    int portLabelYPos = hostLabelYPos;
    portLabel.setBounds(portLabelXPos, portLabelYPos, 100, 20);

    final JTextField port = new JTextField("7800");
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int portXPos = portLabelXPos + 35;
    int portYPos = portLabelYPos;
    port.setBounds(portXPos, portYPos, 100, 20);

    final JLabel headingLabel = new JLabel(AppAnywhereConstants.CLIENT_TITLE);
    headingLabel.setForeground(Color.white);
    headingLabel.setFont(new Font("Arial", Font.BOLD, 18));
    // loginLabel.setFont(new Font("ARIAL", Font.BOLD, 12));
    int headingLabelXPos = buttonXPos;
    int headingLabelYPos = hostLabelYPos - 30;
    headingLabel.setBounds(headingLabelXPos, headingLabelYPos, 200, 20);

    connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String loginName = login.getText().trim();
        final String hostName = host.getText().trim();
        final String portName = port.getText().trim();
        StringBuffer sb = new StringBuffer();
        String sep = "";
        if ("".equals(loginName)) {
          sb.append("login name");
          sep = ", ";
        }
        if ("".equals(hostName)) {
          sb.append(sep);
          sb.append("host");
          sep = ", ";
        }
        if ("".equals(portName)) {
          sb.append(sep);
          sb.append("port");
          sep = ", ";
        }
        if ("".equals(sb.toString())) {
          logger.info("LOGGING IN " + login);
          clientFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          new SwingWorker() {

            public Object construct() {
              connect(loginName, hostName, portName);
              return null;
            }

            public void finished() {
              try {
                clientFrame.getContentPane().remove(loginPanel);
                clientFrame.getContentPane().validate();

              } finally {
                clientFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
              }
            }
          }.start();
        } else {
          JOptionPane.showMessageDialog(clientFrame, "Please enter " + sb, "Warning", JOptionPane.WARNING_MESSAGE);
        }
      }
    });
    clientFrame.getContentPane().setFocusable(true);
    loginPanel.add(loginLabel);
    loginPanel.add(login);
    loginPanel.add(hostLabel);
    loginPanel.add(host);
    loginPanel.add(portLabel);
    loginPanel.add(port);
    loginPanel.add(headingLabel);
    loginPanel.add(connectButton);
    loginPanel.add(imageLabel);
    clientFrame.getContentPane().add(loginPanel);
    clientFrame.setBounds(100, 100, clientFrameSize.width, clientFrameSize.height);
    clientFrame.setResizable(false);
    clientFrame.setVisible(true);

  }

  private synchronized void resetClientFrame() {
    if (clientLabel != null) {
      clientFrame.getContentPane().remove(clientLabel);
    }
    clientFrame.getContentPane().add(loginPanel);
    clientFrame.setSize(clientFrameSize.width, clientFrameSize.height);
    clientFrame.setTitle(AppAnywhereConstants.CLIENT_TITLE);

    clientFrame.getContentPane().validate();
    state.setControl(State.PASSIVE_CONTROL);
  }

  /**
   * For client side only, adds the state change listener to the supplied frame,
   * which will display the name of the controlling user in the title bar of the
   * frame
   *
   * @param frame
   */
  private void addStateChangeListener(final JFrame frame) {
    state.addStateChangeListener(new StateChangeListener() {
      public void stateChanged(short eventType, Object oldState, Object newState) {
        logger.debug("AppAnywhereController.stateChanged " + eventType + ": was:" + oldState + " now:" + newState);
        if (eventType == Command.REQUEST_CONTROL_ID || eventType == State.CONTROL_NOTIFICATION_EVENT) {
          if (newState != null && !((String) newState).trim().equals("")) {
            logger.debug("AppAnywhereController.stateChanged VS7 Anywhere     " + newState + " has control. Was "
                + oldState);
            frame.setTitle("VS7 Anywhere     " + newState + " has control");
            // state.setControllingUser((String) newState);
          }
        }
      }
    });
  }

  private synchronized void setImage(Image image) {
    if (clientLabel == null) {
      imageIcon.setImage(image);
      clientLabel = new JLabel(imageIcon);
    } else {
      Image oldImage = imageIcon.getImage();
      if (oldImage != null) {
        oldImage.flush();
      }
      imageIcon.setImage(image);
      clientFrame.getContentPane().remove(clientLabel);
      // remove the old one
    }
    if (channel != null && channel.isConnected()) {
      clientFrame.getContentPane().add(clientLabel);
      clientLabel.repaint();
      clientFrame.pack();
    }
  }

  public JFrame getServerFrame() {
    return serverFrame;
  }

  public JFrame getClientFrame() {
    return clientFrame;
  }

  private void addEventListener() {
    awtEventListener = new AWTEventListener() {
      public void eventDispatched(AWTEvent awtEvent) {
        // only event processing responsibility of passive side is to pass
        // double clicks
        logger.debug("AppAnywhereController.eventDispatched state:"
            + (state.isActive() ? "ACTIVE" : "PASSIVE " + awtEvent));
        if (state.isActive()) {
          // This side is active - pass events to queue
          // We don't send MouseWheel events - wrong serialVersionUID
          if (awtEvent instanceof MouseEvent) {
            if (!(awtEvent instanceof MouseWheelEvent)) {
              MouseEvent event = (MouseEvent) awtEvent;
              Point point = relativeToFrame(event);
              point.x = point.x - getFrame().getInsets().left - getFrame().getInsets().right;
              point.y = point.y - getFrame().getInsets().top - getFrame().getInsets().bottom;
              if (isMouseMotionEvent(event)) {
                if (!mouseDisabled) {
                  postPoint(point);
                }
              } else {
                // filter out double clicks, we don't want other clients to
                // interpret our double click as their own control-grab
                if (event.getClickCount() <= 1) {
                  // redo the mouse event
                  MouseEvent newMouseEvent = new MouseEvent(event.getComponent(), event.getID(), event.getWhen(), event
                      .getModifiers(), point.x, point.y, event.getClickCount(), event.isPopupTrigger(), event
                      .getButton());
                  logger.debug("AppAnywhereController.eventDispatched " + awtEvent);
                  postEvent(newMouseEvent);
                }
              }
            }
          } else if (awtEvent instanceof KeyEvent && state.isClientSide()) {
            // (on server, no need to pass key events to client, client is just
            // displaying mouse motions and imagePixels updates)s
            logger.debug("AppAnywhereController.eventDispatched Posting KeyEvent:" + awtEvent);
            postEvent(awtEvent);
          }
        } else if (state.isPassive()) {
          // This side is passive, we only care about double clicks, which
          // toggle our control to active
          if (awtEvent instanceof MouseEvent) {
            MouseEvent event = (MouseEvent) awtEvent;
            logger.debug("AppAnywhereController.eventDispatched " + awtEvent);
            if (event.getClickCount() > 1) {
              if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                state.grabControl();
                postCommand(new Command(Command.REQUEST_CONTROL_ID, state.getUser()));
              }
            }
          }
        }
      }
    };
    Toolkit.getDefaultToolkit().addAWTEventListener(awtEventListener, -1);
  }

  private void removeEventListener() {
    if (awtEventListener != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener);
    }
  }

  private boolean isMouseMotionEvent(MouseEvent event) {
    return event.getID() == MouseEvent.MOUSE_MOVED || event.getID() == MouseEvent.MOUSE_DRAGGED
        || event.getID() == MouseEvent.MOUSE_ENTERED || event.getID() == MouseEvent.MOUSE_EXITED;
  }

  /**
   * Translates the coordinates of this mouse event to be relative to the
   * application parent frame
   *
   * @param event
   * @return Translated coordinates of this mouse event relative to the
   *         application parent frame
   */
  private Point relativeToFrame(MouseEvent event) {
    Point point = event.getPoint();
    if (state.isServerSide()) {
      Object object = event.getSource();
      while (object instanceof Component) {
        // we only do this for server side
        if (object == getFrame()) {
          return point;
        }
        Component comp = (Component) object;
        point.x += comp.getLocation().x;
        point.y += comp.getLocation().y;
        object = comp.getParent();
      }
    }

    return point;
  }

  private JFrame getFrame() {
    return clientFrame != null ? clientFrame : serverFrame;
  }

  public void stopController() {
    synchronized (OUTGOING_QUEUE) {
      running = false;
      OUTGOING_QUEUE.notifyAll();
      try {
        channel.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Disconnects a client. Leaves threads running ans server socket listening
   * for new connections
   */
  public synchronized void disconnect() {
    if (channel != null && channel.isConnected()) {
      if (serverFrameTitle != null) {
        serverFrame.setTitle(serverFrameTitle);
      }
      if (state.isClientSide() && equals(state.getUser(), state.getControllingUser())) {
        // if the controlling user disconnects, forefeit control back to the
        // server
        forfeitControlToServer();
      }
      if (state.isServerSide()) {
        // if the controlling user disconnects, forefeit control back to the
        // server
        notifyClientsServerDisconnected();
      } else {
        resetClientFrame();
      }
      // remove the awt event listener
      removeEventListener();
      try {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        // todo get this back on the client side. For now, it is hanging if the
        // server disconnects then the client disconnects
        /*
         * if(state.isServerSide()) { // channel.startFlush(0, true);
         * if(channel.isConnected()) { channel.disconnect(); } channel.close(); }
         */
        if (state.isServerSide()) {
          logger.info("disconnect");
          channel.disconnect();
        }
        logger.info("close");
        channel.close();
        channel = null;
        logger.info("interrupt");
        transmitThread.interrupt();
        logger.info("join");
        transmitThread.join();
        logger.info("done");
      } catch (Exception e) {
        e.printStackTrace();
      }
      // updateStatsFrame();
      synchronized (OUTGOING_QUEUE) {
        OUTGOING_QUEUE.clear();
      }
    }
  }

  private boolean equals(String object1, String object2) {
    return object1 == object2 || object1 != null && object2 != null && object1.equals(object2);
  }

  /**
   * Notify clients server is off. They must disconnect
   */
  private void notifyClientsServerDisconnected() {
    Command commmand = new Command(Command.SERVER_DISCONNECTED_ID);
    postCommand(commmand);
  }

  private void forfeitControlToServer() {
    Command commmand = new Command(Command.FORFEIT_CONTROL_ID);
    postCommand(commmand);
  }

  private void startThreads(JFrame frame) {
    startTransmitterThread();
    // startReceiverThread();
  }

  /**
   * Picks up items from the queue and transmits them to the other side
   */
  private void startTransmitterThread() {
    transmitThread = new Thread(new Runnable() {

      public void run() {
        try {
          while (running) {
            try {
              synchronized (OUTGOING_QUEUE) {
                while (running && OUTGOING_QUEUE.isEmpty()) {
                  OUTGOING_QUEUE.wait();
                }
                if (!OUTGOING_QUEUE.isEmpty()) {
                  Object object = OUTGOING_QUEUE.remove(0);
                  transmit(object);
                  logger.debug("AppAnywhereContoller.run processing " + object);
                  // updateStatsFrame();
                  if (object instanceof ImageStructure) {
                    logger.debug("AppAnywhereContoller.run posting imagePixels:" + object);
                    ((ImageStructure) object).dispose();
                  }
                }
                logger.debug("AppAnywhereContoller.run done processing. Waiting for more");
              }
            } catch (ChannelException e) {
              e.printStackTrace();
              // disconnect this client, but continue looping for new client
              disconnect();
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    transmitThread.setDaemon(true);
    transmitThread.start();
  }

  /**
   * Transmits the object synchronously. Actually, it posts it to the channel
   * synchronously, but the chanel returns before the object might be completely
   * transmitted
   *
   * @param object
   * @throws ChannelNotConnectedException
   * @throws ChannelClosedException
   */
  private void transmit(Object object) throws ChannelNotConnectedException, ChannelClosedException {
    if (channel != null && channel.isConnected()) {
      channel.send(null, null, (Serializable) object);
    }
  }

  /**
   * Receives items from the other side, and translates them
   */
  private void startReceiverThread() {
    Thread thread = new Thread(new Runnable() {

      public void run() {
        try {
          while (running) {
            synchronized (INCOMING_QUEUE) {
              while (running && INCOMING_QUEUE.isEmpty()) {
                INCOMING_QUEUE.wait();
              }
              if (!INCOMING_QUEUE.isEmpty()) {
                Object message = INCOMING_QUEUE.remove(0);
                processingIncomingObject(message);
              }
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (IOException e) {
          e.printStackTrace();
        }
        logger.info("AppAnywhereContoller.run exiting");
      }

    });
    thread.setDaemon(true);
    thread.start();
  }

  private void startCatatonicMouseThread() {
    new CatonicMouseThread(state, robot);
  }

  /**
   * Translates the point, then moves the mouse to the translated x,y coordinate
   *
   * @param point
   *          the point to be translated, then moves the mouse to the translated
   *          x,y coordinate
   */
  private void translateAndMoveMouse(Point point) {
    translateIncoming(point);
    robot.mouseMove(point.x, point.y);
    state.setLastMouseEvent(point);
  }

  /**
   * Processes incoming objects read from the ObjectInputStream
   *
   * @param object
   */
  private void processingIncomingObject(Object object) throws IOException {
    // updateStatsFrame("Processing incoming");
    if (object instanceof Command) {
      processIncomingCommand((Command) object);
    } else if (object instanceof MouseEvent) {
      if (isShowing()) {
        processIncomingMouseEvent((MouseEvent) object);
      }
    } else if (object instanceof KeyEvent) {
      if (isShowing()) {
        processIncomingKeyEvent((KeyEvent) object);
      }
    } else if (object instanceof Point) {
      if (isShowing()) {
        processIncomingPoint((Point) object);
      }
    } else if (object instanceof ImageStructure) {
      processIncomingImageStructure((ImageStructure) object);
    }
    // updateStatsFrame();
  }

  private void processIncomingImageStructure(final ImageStructure incomingImageStructure) {
    // Pixels represent the diff. Apply to our imagePixels
    if (state.isClientSide()) {
      new Thread(new Runnable() {
        public void run() {
          try {
            incomingImageStructure.decompress();
            boolean ischanged = state.applyIncomingImageStructure(incomingImageStructure);
            if (ischanged) {
              Image image = incomingImageStructure.getImage();
              if (image != null) {
                repaintClientFrame(image);
                incomingImageStructure.dispose();
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();
    }
  }

  private void repaintClientFrame(Image image) {
    if (clientFrame != null) {
      setImage(image);
    }
  }

  /**
   * MouseEvent has just arrived over the network. If we are active, then ignore
   * it, unless it is a double click
   *
   * @param event
   */
  private void processIncomingMouseEvent(MouseEvent event) {
    if (state.isActive()) {
      // if we are active, only events we care about are client request for
      // control
    } else if (isMouseMotionEvent(event)) {
      // we are passive - move the mouse to the designated spot
      Point point = new Point(event.getX(), event.getY());
      translateAndMoveMouse(point);
    } else if (event.getID() == MouseEvent.MOUSE_PRESSED &&
        ImageChangeDetector.isWorking(getFrame()) &&
        getFrame().getBounds().contains(MouseInfo.getPointerInfo().getLocation())) {
      logger.info("Mouse pressed on "+getFrame().getTitle());
      int buttonsFlag = event.getButton();
      // JDK1.5 compatability
      if (buttonsFlag == 1 || buttonsFlag == 16) {
        buttonsFlag = InputEvent.BUTTON1_MASK;
      }
      try {
        robot.mousePress(buttonsFlag);
      }
      catch (IllegalArgumentException e) {
        logger.info("AppAnywhereContoller.processIncomingMouseEvent can't process flags:"+ buttonsFlag);
      }
    } else if (event.getID() == MouseEvent.MOUSE_RELEASED &&
        ImageChangeDetector.isWorking(getFrame()) &&
        getFrame().getBounds().contains(MouseInfo.getPointerInfo().getLocation())) {
      logger.info("Mouse released on "+getFrame().getTitle());
      int buttonsFlag = event.getButton();
        // JDK1.5 compatability
        if (buttonsFlag == 1 || buttonsFlag == 16) {
          buttonsFlag = InputEvent.BUTTON1_MASK;
        }
        try {
          robot.mouseRelease(buttonsFlag);
        }
        catch (IllegalArgumentException e) {
          logger.info("AppAnywhereContoller.processIncomingMouseEvent can't process flags:" + buttonsFlag);
        }
    } else if (event.getID() == MouseEvent.MOUSE_WHEEL &&
        ImageChangeDetector.isWorking(getFrame()) &&
        getFrame().getBounds().contains(MouseInfo.getPointerInfo().getLocation())) {
      robot.mouseWheel(((MouseWheelEvent) event).getScrollAmount());
    }
    // a hack - assume mouse motions are coming in clusters, so the next item
    // will likely be a mouse event
    try {
      Thread.sleep(INCOMING_MOUSE_DELAY);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * KeyEvent has just arrived over the network. If we are active, then ignore it
   *
   * @param event
   */
  private void processIncomingKeyEvent(KeyEvent event) {
    logger.debug("AppAnywhereContoller.processIncomingKeyEvent received key event " + event);
    if (state.isActive()) {
      // process key stroke for client requesting control. Right now, there is
      // no such keystroke.
    } else {
      // we are passive - dispatch the event
      logger.debug("AppAnywhereContoller.processIncomingKeyEvent pressing key " + event);
      if (event.getID() == KeyEvent.KEY_PRESSED) {
        robot.keyPress(event.getKeyCode());
      } else if (event.getID() == KeyEvent.KEY_RELEASED) {
        robot.keyRelease(event.getKeyCode());
      }
    }
  }

  private void processIncomingCommand(Command command) {
    if (command.getId() == Command.REQUEST_CONTROL_ID) {
      logger.debug("AppAnywhereContoller.processIncomingCommand " + command);
      state.forfeitControl((String) command.getObject());
    } else if (command.getId() == Command.FORFEIT_CONTROL_ID) {
      // client is forfeiting. If we are the server, grab control
      if (state.isServerSide()) {
        state.grabControl();
      }
    } else if (command.getId() == Command.REQUEST_CONTROLLER_ID) {
      // client just signed on. Is requesting controller from server
      logger.debug("AppAnywhereContoller.processIncomingCommand " + command);
      if (state.isServerSide()) {
        logger.debug("AppAnywhereController.processIncomingCommand received request for controlling user:"
            + state.getControllingUser());
        postCommand(new Command(Command.CONTROLLER_NAME_NOTIFICATION_ID, state.getControllingUser()));
      }
    } else if (command.getId() == Command.CONTROLLER_NAME_NOTIFICATION_ID) {
      // client just signed on. Is requesting controller from server
      String controllingUser = (String) command.getObject();
      logger.debug("AppAnywhereController.processIncomingCommand received controlling user:" + controllingUser);
      state.setControllingUser(controllingUser);
    } else if (command.getId() == Command.CLIENT_RESPOST_IMAGE) {
      if (state.isServerSide()) {
        imageChangeDetector.schedulePostImageDiffForced();
      }
    } else if (command.getId() == Command.SERVER_DISCONNECTED_ID) {
      disconnect();
    }
  }

  private void processIncomingPoint(Point point) {
    if (state.isActive()) {
      // if we are active, only events we care about are client request for
      // control
    } else {
      // we are passive - move the mouse to the designated spot
      translateAndMoveMouse(point);
    }

  }

  /**
   * Translates the incoming point relative to our bounds
   *
   * @param p
   */
  private void translateIncoming(Point p) {
    JFrame frame = getFrame();
    Rectangle bounds = frame.getBounds();
    p.x += bounds.x + getFrame().getInsets().left + getFrame().getInsets().right;
    p.y += bounds.y + getFrame().getInsets().top + getFrame().getInsets().bottom;

  }

  /**
   * Posts mouse events to the processing queue to be transmitted asynchronously
   * to the other side.
   *
   * @param awtEvent
   */
  private void postEvent(AWTEvent awtEvent) {
    // we specifically don't want to send MouseWheel events - wrong
    // serialVersionUID
    if (state.isActive()) {
      if (awtEvent instanceof MouseEvent) {
        if (!(awtEvent instanceof MouseWheelEvent)) {
          logger.debug("AppAnywhereController.postPoint adding to queue:" + awtEvent);
          addToQueue(awtEvent);
        }
      } else if (awtEvent instanceof KeyEvent) {
        if (!excludeKeyEvent(((KeyEvent) awtEvent))) {
          addToQueue(awtEvent);
        }
      }
    }
  }

  /**
   * Some events should not be transmitted, such as F4 (which will close the
   * server app). This method returns true if an event should be ignored.
   *
   * @param event
   * @return true if an event should be ignored.
   */
  private boolean excludeKeyEvent(KeyEvent event) {
    boolean rval = false;
    if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0) {
      // alt key pressed - ignore
      rval = true;
    } else if (event.getKeyChar() == KeyEvent.VK_1) {
      // Help key pressed - ignore
      rval = true;
    }
    return rval;
  }

  /**
   * Posts the {@link Command} to the queue
   *
   * @param command
   */
  private void postCommand(Command command) {
    logger.debug("AppAnywhereContoller.postCommand " + command);
    addToQueue(command);
  }

  /**
   * Posts mouse relocation commands (in the form of a Point) to the processing
   * queue to be transmitted asynchronously to the other side.
   *
   * @param point
   */
  private void postPoint(Point point) {
    logger.debug("AppAnywhereController.postPoint adding to queue:" + point);
    addToQueue(point);
    logger.debug("AppAnywhereContoller.postPoint added point to outgoing queue " + point);
  }

  /**
   * Posts imagePixels diffs to the processing queue to be transmitted
   * asynchronously to the other side.
   *
   * @param currentWidth
   * @param currentHeight
   * @param diffs
   * @param imageType
   */
  public void postImageDiffs(int currentWidth, int currentHeight, int[] diffs, int imageType) throws IOException {
    ImageStructure imageStructure = new ImageStructure(currentWidth, currentHeight, diffs, imageType);
    imageStructure.compress();
    addToQueue(imageStructure);
    logger.debug("AppAnywhereContoller.postImageDiffs added imagePixels to outgoing queue " + currentWidth + ","
        + currentHeight);
  }

  /**
   * Returns true if the frame is not hidden. Otherwise it should not transmit
   * or receive. Note: if the frame is iconified, isFocused == false, even if
   * the app is selected by ALT-ESC todo: Fix this - does not detect modal
   * dialogs!
   *
   * @return true if the frame is not hidden. Otherwise it should not transmit
   *         or receive.
   */
  private boolean isShowing() {
    return showing;
  }

  private void setShowing(boolean showing) {
    this.showing = showing;
  }

  private void addToQueue(Object object) {
    if (isShowing()) {
      synchronized (OUTGOING_QUEUE) {
        OUTGOING_QUEUE.add(object);
        // updateStatsFrame();
        OUTGOING_QUEUE.notifyAll();
      }
    } else {
      logger.debug("AppAnywhereController.addToQueue frame is not showing - not posting " + object);
    }
  }

}
/*
 * $Log: AppAnywhereController.java,v $
 * Revision 1.43  2007/12/12 10:08:26  gmalik2
 * Fixing the Mouse Pointer Allignment with insets
 *
 * Revision 1.42  2007/12/10 13:08:42  gmalik2
 * Fixing the mouse click issue.
 *
 * Revision 1.41  2007/12/06 10:32:30  gmalik2
 * changing the log information
 *
 * Revision 1.40  2007/12/04 08:51:42  gmalik2
 * Handling mouse event issues
 *
 * Revision 1.39  2007/12/03 08:11:01  gmalik2
 * 1. If mouse is outside the passive frame, mouse click is not allowed
 * 2. If passive frame is not focussed, mouse click is not allowed
 *
 * Revision 1.38  2007/11/29 10:54:29  gmalik2
 * Added titilebar height to mouse movement area
 *
 * Revision 1.37  2007/10/31 10:04:12  gmalik2
 * Refactoring
 * Revision 1.36 2007/10/24 08:09:20
 * gmalik2 1. Created an object of ImageChangeDetector 2. Adding the command
 * 'Client Repost Image' to the queue. Revision 1.35 2007/10/18 12:07:01 gmalik2
 * Fixing the performance issue Revision 1.34 2007/07/26 22:39:32 vgrazi Added
 * some Javadoc Revision 1.33 2006/11/14 15:11:21 vgrazi Now resetting title bar
 * when client disconnects Now hiding immediately when client shuts down
 * Revision 1.32 2006/11/14 02:58:01 jli17 remove event listener when disconnect
 * Revision 1.31 2006/11/13 17:20:28 jli17 reset the state for client Revision
 * 1.30 2006/11/09 21:29:40 vgrazi Made "showing" volatile - accessed by
 * different threads Revision 1.29 2006/11/09 21:29:04 vgrazi Added som
 * synchronization for image setting Revision 1.28 2006/11/09 20:00:44 vgrazi
 * Removed GlassPane work Revision 1.27 2006/11/09 19:00:30 jli17 fixed for the
 * client frame size problem. Revision 1.26 2006/11/09 17:21:44 vgrazi Now
 * inserted client frame components in GlassFrame - problems with key capture!
 * Revision 1.25 2006/11/09 16:11:58 jli17 Added WindowFocusListener to not show
 * anything else in case the server frame lost focus. Revision 1.24 2006/11/09
 * 01:48:03 vgrazi disconnect() waits 10ms before disconnecting, to give
 * disconnect message time to percolate thru the network Revision 1.23
 * 2006/11/08 21:46:06 vgrazi Some control tweaks Revision 1.22 2006/11/08
 * 21:00:07 jli17 Added viewAccepted to show joined/left member. Revision 1.21
 * 2006/11/08 17:09:06 vgrazi Adjusted translate method for new server transmit,
 * which is now clipping off insets before transmitting image Revision 1.20
 * 2006/11/08 16:19:37 vgrazi Now filtering out unwanted operations, such as
 * client closing the server app No longer transmitting unless server has focus
 * Transmit handling improved KeyEvents posting corrected Revision 1.19
 * 2006/11/07 19:32:08 vgrazi Now filtering out double clicks Revision 1.18
 * 2006/11/07 16:58:50 jli17 Not use receiver thread for now. Revision 1.17
 * 2006/11/07 16:43:56 vgrazi Added client frame login Revision 1.16 2006/11/06
 * 23:15:35 vgrazi Moved input message processing to a thread, for improved
 * springiness Revision 1.15 2006/11/06 21:59:47 vgrazi New SwingWorker Revision
 * 1.14 2006/11/06 21:55:55 vgrazi Added SwinWorker Revision 1.13 2006/11/06
 * 21:25:51 vgrazi Displaying WAIT CURSOR on connect request Revision 1.12
 * 2006/11/06 20:57:43 vgrazi Now displaying name of controlling user in client
 * frame title Revision 1.11 2006/11/06 20:50:21 vgrazi Connection now happens
 * by pressing a button Revision 1.10 2006/11/06 18:28:12 jli17 checking the
 * clienFrame before repaint the image in case of the image comes in before
 * clientFrame is ready. Revision 1.9 2006/11/06 15:49:48 vgrazi Moved client
 * and server launching to start() method for better control Revision 1.8
 * 2006/11/03 19:02:18 jli17 remove viewAccepted method in ReceiverAdapter
 * Revision 1.7 2006/11/03 18:55:34 vgrazi Reformatted Revision 1.6 2006/11/03
 * 18:53:12 jli17 Use receiver instead of receive thread Revision 1.5 2006/11/03
 * 17:56:06 vgrazi Reformatted Revision 1.4 2006/11/03 17:55:43 vgrazi Added
 * $Log
 */