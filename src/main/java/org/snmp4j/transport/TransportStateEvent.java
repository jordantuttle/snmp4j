/*_############################################################################
  _## 
  _##  SNMP4J 2 - TransportStateEvent.java  
  _## 
  _##  Copyright (C) 2003-2013  Frank Fock and Jochen Katz (SNMP4J.org)
  _##  
  _##  Licensed under the Apache License, Version 2.0 (the "License");
  _##  you may not use this file except in compliance with the License.
  _##  You may obtain a copy of the License at
  _##  
  _##      http://www.apache.org/licenses/LICENSE-2.0
  _##  
  _##  Unless required by applicable law or agreed to in writing, software
  _##  distributed under the License is distributed on an "AS IS" BASIS,
  _##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  _##  See the License for the specific language governing permissions and
  _##  limitations under the License.
  _##  
  _##########################################################################*/
package org.snmp4j.transport;

import org.snmp4j.smi.Address;

import java.io.IOException;
import java.util.EventObject;

/**
 * The <code>TransportStateEvent</code> describes a state change for
 * a transport connection. Optionally, connection establishment can be
 * cancelled.
 *
 * @author Frank Fock
 * @version 1.8
 * @since 1.7
 */
public class TransportStateEvent extends EventObject {
  private static final long serialVersionUID = 6440139076579035559L;

  public enum TransportStates {
    UNKNOWN,
    CONNECTED,
    DISCONNECTED_REMOTELY,
    DISCONNECTED_TIMEOUT,
    CLOSED
  }

  private final TransportStates newState;
  private final Address peerAddress;
  private final IOException causingException;

  private boolean cancelled = false;

  public TransportStateEvent(TcpTransportMapping source,
                             Address peerAddress,
                             TransportStates newState,
                             IOException causingException) {
    super(source);
    this.newState = newState;
    this.peerAddress = peerAddress;
    this.causingException = causingException;
  }

  public IOException getCausingException() {
    return causingException;
  }

  public TransportStates getNewState() {
    return newState;
  }

  public Address getPeerAddress() {
    return peerAddress;
  }

  /**
   * Indicates whether this event has been canceled. Only
   * {@link org.snmp4j.transport.TransportStateEvent.TransportStates#CONNECTED} events can be canceled.
   * @return
   *    <code>true</code> if the event has been canceled.
   * @since 1.8
   */
  public boolean isCancelled() {
    return cancelled;
  }

  public String toString() {
    return TransportStateEvent.class.getName()+"[source="+source+
        ",peerAddress="+peerAddress+
        ",newState="+newState+
        ",cancelled="+cancelled+
        ",causingException="+causingException+"]";
  }

  /**
   * Sets the canceled state of the transport event. Only
   * {@link org.snmp4j.transport.TransportStateEvent.TransportStates#CONNECTED} events can be canceled.
   * @param cancelled
   *    <code>true</code> if the event should be canceled, i.e. a connection
   *    attempt should be rejected.
   * @since 1.8
   */
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
