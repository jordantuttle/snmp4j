/*_############################################################################
  _## 
  _##  SNMP4J 2 - AbstractTransportMapping.java  
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

import org.snmp4j.MessageDispatcher;
import org.snmp4j.TransportMapping;
import org.snmp4j.TransportStateReference;
import org.snmp4j.smi.Address;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>AbstractTransportMapping</code> provides an abstract
 * implementation for the message dispatcher list and the maximum inbound
 * message size.
 *
 * @author Frank Fock
 * @version 2.0
 */
public abstract class AbstractTransportMapping<A extends Address>
    implements TransportMapping<A> {

  protected List<TransportListener> transportListener = new ArrayList<>(1);
  protected int maxInboundMessageSize = (1 << 16) - 1;

  @Override
  public synchronized void addTransportListener(TransportListener l) {
    if (!transportListener.contains(l)) {
      List<TransportListener> tlCopy =
          new ArrayList<>(transportListener);
      tlCopy.add(l);
      transportListener = tlCopy;
    }
  }

  @Override
  public synchronized void removeTransportListener(TransportListener l) {
    if (transportListener != null && transportListener.contains(l)) {
      List<TransportListener> tlCopy =
          new ArrayList<>(transportListener);
      tlCopy.remove(l);
      transportListener = tlCopy;
    }
  }

  protected void fireProcessMessage(Address address,  ByteBuffer buf,
                                    TransportStateReference tmStateReference) {
    if (transportListener != null) {
      for (TransportListener aTransportListener : transportListener) {
        aTransportListener.processMessage(this, address, buf, tmStateReference);
      }
    }
  }

  /**
   * Gets the inbound buffer size for incoming requests. When SNMP packets are
   * received that are longer than this maximum size, the messages will be
   * silently dropped and the connection will be closed.
   * @return
   *    the maximum inbound buffer size in bytes.
   */
  @Override
  public int getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }
}
