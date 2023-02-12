package com.vgrazi.pca;

import java.io.Serializable;

public class Command implements Serializable {

  private static final long serialVersionUID = 1581006171750706892L;

  private int id;
  private final Serializable value;

  public final static short REQUEST_CONTROL_ID = 1;
  public final static short FORFEIT_CONTROL_ID = 2;
  public final static short CONTROLLER_NAME_NOTIFICATION_ID = 3;
  public final static short REQUEST_CONTROLLER_ID = 4;
  public static final int SERVER_DISCONNECTED_ID = 5;
  public static final int CLIENT_RESPOST_IMAGE = 6;

  public Command(int id) {
    this(id, null);
  }

  public Command(int id, Serializable value) {
    this.id = id;
    this.value = value;
  }

  public String toString() {
    Serializable object = "";
    if (value != null) {
      object = value;
    }
    switch (id) {
    case REQUEST_CONTROL_ID:
      return id + ": REQUEST_CONTROL " + object;
    case FORFEIT_CONTROL_ID:
      return id + ":FORFEIT_CONTROL " + object;
    case REQUEST_CONTROLLER_ID:
      return id + ":REQUEST_CONTROLLER " + object;
    case CONTROLLER_NAME_NOTIFICATION_ID:
      return id + ":CONTROLLER_NAME_NOTIFICATION " + object;
    case CLIENT_RESPOST_IMAGE:
      return id + ":CLIENT_RESPOST_IMAGE " + object;
    }
    return "UNKNOWN";
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Command command = (Command) o;

    return id == command.id && !(value != null ? !value.equals(command.value) : command.value != null);

  }

  public int hashCode() {
    int result;
    result = id;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  public int getId() {
    return id;
  }

  public Serializable getObject() {
    return value;
  }
}
/*
 * $Log: Command.java,v $
 * Revision 1.6  2007/10/31 10:04:07  gmalik2
 * Refactoring
 * Revision 1.5 2007/10/24 08:07:55 gmalik2 Adding a new
 * Command: Client Respost Image This command will be send to the server by the
 * client when it initially connects to the server. The server should repost the
 * image back to the client Revision 1.4 2006/11/09 17:06:48 vgrazi Added
 * SERVER_DISCONNECTED_ID Revision 1.3 2006/11/08 21:47:20 vgrazi Controlling
 * user tweaks and added toString() Revision 1.2 2006/11/06 20:54:23 vgrazi
 * REQUEST CONTROL now accepts a User Revision 1.1 2006/10/09 23:50:08 vgrazi
 * Initial revision
 */