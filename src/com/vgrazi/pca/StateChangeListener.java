package com.vgrazi.pca;

/**
 * Created by Victor Grazi. Date: Sep 10, 2006 - 3:20:32 PM
 */
public interface StateChangeListener {

  public void stateChanged(short eventType, Object oldState, Object newState);
}



/**
 *
 * $Log: StateChangeListener.java,v $
 * Revision 1.4  2007/11/22 07:24:33  gmalik2
 * Adding cvs change log information inside the class' source file
 *
 *
 */