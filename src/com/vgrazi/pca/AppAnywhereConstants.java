package com.vgrazi.pca;

public interface AppAnywhereConstants {
  String STATS_FRAME = "STATS_FRAME";
  String MOUSE_DISABLED = "MOUSE_DISABLED";
  String TRANSMIT_DIFFS = "TRANSMIT_DIFFS";
  String props = "tcp.xml";

  String IMAGE_URL = "images/vs7_splash_image.jpg";
  String INIT_TEXT = "<html><TABLE width=\"600\" border=0 align=left>"
      + "<tr height=1><td width=\"10%\">&nbsp;</td><td width=\"90%\">&nbsp;</td></tr>"
      + "<TR rowheight=30><TD>&nbsp;</TD><TD ALIGN=\"LEFT\">&nbsp;</TD></TR>"
      + "<TR rowheight=30><TD>&nbsp;</TD><TD ALIGN=\"LEFT\">&nbsp;</TD></TR>"
      + "<TR rowheight=30><TD>&nbsp;</TD><TD ALIGN=\"LEFT\"><H2>"
      + "<FONT COLOR=\"WHITE\">VS7 Anywhere</FONT></H2>&nbsp;</TD></TR> </TABLE></HTML>";

  String JGROUPS_HOST = "jgroups.tcpping.initial_hosts";
  String CLIENT_TITLE = "VS7 Anywhere";
}
/*
 * $Log: AppAnywhereConstants.java,v $
 * Revision 1.7  2007/10/31 10:04:07  gmalik2
 * Refactoring
 * Revision 1.6 2006/11/14 15:09:47 vgrazi
 * Added CLIENT_TITLE Revision 1.5 2006/11/07 16:46:02 vgrazi Added JGROUPS_HOST
 * Revision 1.4 2006/11/06 20:56:34 vgrazi Added IMAGE_URL and INITI_TEXT -
 * todo: move these to a props file Revision 1.3 2006/11/06 17:28:56 jli17
 * Change to use tcp.xml for JChannel properties Revision 1.2 2006/11/06
 * 15:47:37 vgrazi Moved Multicast properties string here Revision 1.1
 * 2006/10/12 23:06:45 vgrazi Small refactoring
 */