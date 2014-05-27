/*_############################################################################
  _## 
  _##  SNMP4J 2 - TestPrivDES.java  
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


package org.snmp4j.security;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OctetString;

public class TestPrivDES {
  static Logger cat = LoggerFactory.getLogger(TestPrivDES.class.getName());

  public static String asHex(byte buf[]) {
    return new OctetString(buf).toHexString();
  }

  @Test
  public void testEncrypt()
  {
      PrivDES pd = new PrivDES();
      DecryptParams pp = new DecryptParams();
      byte[] key = "1234567890123456".getBytes();
      byte[] plaintext =
          "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".
          getBytes();
      byte[] ciphertext;
      byte[] decrypted;
      int engine_boots = 1;
      int engine_time = 2;

    cat.debug("Cleartext: {}", asHex(plaintext));
      ciphertext = pd.encrypt(plaintext, 0, plaintext.length, key, engine_boots, engine_time, pp);
    cat.debug("Encrypted: {}", asHex(ciphertext));
      decrypted = pd.decrypt(ciphertext, 0, ciphertext.length, key, engine_boots, engine_time, pp);
    cat.debug("Cleartext: {}", asHex(decrypted));

      for (int i = 0; i < plaintext.length; i++) {
        Assert.assertEquals(plaintext[i], decrypted[i]);
      }
    cat.info("pp length is: {}", pp.length);
    Assert.assertEquals(8, pp.length);
    }
}
