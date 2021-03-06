/*_############################################################################
  _## 
  _##  SNMP4J 2 - SnmpTest.java  
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

package org.snmp4j;

import org.junit.*;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.*;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DummyTransport;
import org.snmp4j.transport.TransportMappings;

import java.io.IOException;
import java.util.*;

/**
 * Junit 4 test class for testing the {@link org.snmp4j.Snmp} class. The tests are run
 * against a {@link DummyTransport} which allows to directly link two virtual
 * {@link org.snmp4j.TransportMapping}s used blocking queues.
 *
 * @author Frank Fock
 * @version 2.0
 */
public class SnmpTest {
  private DummyTransport<UdpAddress> transportMappingCG;
  private AbstractTransportMapping<UdpAddress> transportMappingCR;
  private Snmp snmpCommandGenerator;
  private Snmp snmpCommandResponder;
  private CommunityTarget communityTarget =
      new CommunityTarget(GenericAddress.parse("udp:127.0.0.1/161"), new OctetString("public"));
  private UserTarget userTarget =
      new UserTarget(GenericAddress.parse("udp:127.0.0.1/161"), new OctetString("SHADES"), new byte[0]);

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    SNMP4JSettings.setExtensibilityEnabled(true);
    SecurityProtocols.getInstance().addDefaultProtocols();
    System.setProperty(TransportMappings.TRANSPORT_MAPPINGS, "dummy-transports.properties");
    Assert.assertEquals(DummyTransport.class,
        TransportMappings.getInstance().createTransportMapping(GenericAddress.parse("udp:127.0.0.1/161")).getClass());
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception  {
    SecurityProtocols.setSecurityProtocols(null);
    System.setProperty(TransportMappings.TRANSPORT_MAPPINGS, "");
    SNMP4JSettings.setExtensibilityEnabled(false);
  }

  @Before
  public void setUp() throws Exception {
    transportMappingCG = new DummyTransport<>(new UdpAddress("127.0.0.1/4967"));
    transportMappingCR = transportMappingCG.getResponder(new UdpAddress("127.0.0.1/161"));
    snmpCommandGenerator = new Snmp(transportMappingCG);
    MPv3 mpv3CG = (MPv3) snmpCommandGenerator.getMessageDispatcher().getMessageProcessingModel(MPv3.ID);
    mpv3CG.setLocalEngineID(MPv3.createLocalEngineID(new OctetString("generator")));
    SecurityModels.getInstance().addSecurityModel(
        new USM(SecurityProtocols.getInstance(), new OctetString(mpv3CG.getLocalEngineID()), 0));
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHADES"), AuthSHA.ID, new OctetString("_12345678_"),
            PrivDES.ID, new OctetString("_0987654321_")));
    snmpCommandResponder = new Snmp(transportMappingCR);
    CounterSupport.getInstance().addCounterListener(new DefaultCounterListener());
    SecurityModels respSecModels = new SecurityModels() {

    };
    MPv3 mpv3CR = (MPv3) snmpCommandResponder.getMessageDispatcher().getMessageProcessingModel(MPv3.ID);
    mpv3CR.setLocalEngineID(MPv3.createLocalEngineID(new OctetString("responder")));
    respSecModels.addSecurityModel(new USM(SecurityProtocols.getInstance(),
                                           new OctetString(mpv3CR.getLocalEngineID()), 0));
    mpv3CR.setSecurityModels(respSecModels);
    snmpCommandResponder.getUSM().addUser(
        new UsmUser(new OctetString("SHADES"), AuthSHA.ID, new OctetString("_12345678_"),
        PrivDES.ID, new OctetString("_0987654321_")));
  }

  @After
  public void tearDown() throws IOException {
    snmpCommandGenerator.close();
    snmpCommandResponder.close();
  }

  @Test
  public void testSmiConstants() {
    int[] definedConstants = new int[] {
        SMIConstants.SYNTAX_INTEGER,
        SMIConstants.SYNTAX_OCTET_STRING,
        SMIConstants.SYNTAX_NULL,
        SMIConstants.SYNTAX_OBJECT_IDENTIFIER,
        SMIConstants.SYNTAX_IPADDRESS,
        SMIConstants.SYNTAX_INTEGER32,
        SMIConstants.SYNTAX_COUNTER32,
        SMIConstants.SYNTAX_GAUGE32,
        SMIConstants.SYNTAX_UNSIGNED_INTEGER32,
        SMIConstants.SYNTAX_TIMETICKS,
        SMIConstants.SYNTAX_OPAQUE,
        SMIConstants.SYNTAX_COUNTER64
    };
    String[] constantNames = new String[] {
        "INTEGER",
        "OCTET_STRING",
        "NULL",
        "OBJECT_IDENTIFIER",
        "IPADDRESS",
        "INTEGER32",
        "COUNTER32",
        "GAUGE32",
        "UNSIGNED_INTEGER32",
        "TIMETICKS",
        "OPAQUE",
        "COUNTER64"
    };
    for (int i=0; i<definedConstants.length; i++) {
      System.out.println(constantNames[i] + " = "+definedConstants[i]);
    }
    for (int i=0; i<definedConstants.length; i++) {
      System.out.println(constantNames[i]);
    }
    for (int definedConstant : definedConstants) {
      System.out.println(definedConstant);
    }
  }

  @Test
  public void testListen() throws IOException {
    Assert.assertEquals(transportMappingCG.isListening(), false);
    snmpCommandGenerator.listen();
    Assert.assertEquals(transportMappingCG.isListening(), true);
  }

  @Test
  public void testClose() throws Exception {
    Assert.assertEquals(transportMappingCG.isListening(), false);
    snmpCommandGenerator.close();
    Assert.assertEquals(transportMappingCG.isListening(), false);
    testListen();
    snmpCommandGenerator.close();
    Assert.assertEquals(transportMappingCG.isListening(), false);
  }

  /*
  @Test
  public void testGetV1() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version1);
    PDU pdu = new PDU();
    pdu.setType(PDU.GET);
    addTestVariableBindings(pdu, false, false, target.getVersion());
    syncRequestTest(target, pdu);
  }

  @Test
  public void testGetV2c() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version2c);
    PDU pdu = new PDU();
    pdu.setType(PDU.GET);
    addTestVariableBindings(pdu, false, false, target.getVersion());
    syncRequestTest(target, pdu);
  }

  @Test
  public void testGetV3() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(10000);
    target.setVersion(SnmpConstants.version3);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    pdu.setContextName(new OctetString("myContext"));
    addTestVariableBindings(pdu, false, false, target.getVersion());
    syncRequestTest(target, pdu);
  }

  @Test
  public void testGetV3_RFC3414_3_2_3() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(1000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityName(new OctetString(""));
    target.setAuthoritativeEngineID(new byte[0]);
    final ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    // test it
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    snmpCommandGenerator.setReportHandler((pduHandle, event) -> {
      PDU expectedResponse = makeReport(pdu, new VariableBinding(SnmpConstants.usmStatsUnknownEngineIDs, new Counter32(1)));
      // request ID will be 0 because ScopedPDU could not be parsed:
      expectedResponse.setRequestID(new Integer32(0));
      ((ScopedPDU)expectedResponse).setContextEngineID(new OctetString(snmpCommandResponder.getUSM().getLocalEngineID()));
      Assert.assertEquals(expectedResponse, event.getPDU());
    });
    // first try should return local error
    try {
      ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
      Assert.assertNull(resp.getResponse());
    } catch (MessageException mex) {
      Assert.assertEquals(SnmpConstants.SNMPv3_USM_UNKNOWN_SECURITY_NAME, mex.getSnmp4jErrorStatus());
    }
  }


  @Test
  public void testGetV3_RFC3414_3_2_4() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(1000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityName(new OctetString("unknownSecurityName"));
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    addTestVariableBindings(pdu, false, false, target.getVersion());
    // test it
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    // first try should return local error
    try {
      ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
      Assert.assertNull(resp);
    } catch (MessageException mex) {
      Assert.assertEquals(SnmpConstants.SNMPv3_USM_UNKNOWN_SECURITY_NAME, mex.getSnmp4jErrorStatus());
    }
    // second try: remote error
    target.setSecurityName(new OctetString("SHAAES"));
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHAAES"), AuthSHA.ID, new OctetString("_12345678_"),
            PrivAES128.ID, new OctetString("_0987654321_")));

    ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
    PDU expectedResponse = makeReport(pdu, new VariableBinding(SnmpConstants.usmStatsUnknownUserNames, new Counter32(1)));
    // request ID will be 0 because ScopedPDU could not be parsed:
    expectedResponse.setRequestID(new Integer32(0));
    ((ScopedPDU)expectedResponse).setContextEngineID(new OctetString(snmpCommandResponder.getUSM().getLocalEngineID()));
    Assert.assertEquals(expectedResponse, resp.getResponse());

  }
  */

  @Test
  public void testUsmSeparation() {
    Assert.assertNotSame(snmpCommandGenerator.getUSM(), snmpCommandResponder.getUSM());
  }

  /*
  @Test
  public void testGetV3_RFC3414_3_2_5() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(1000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    addTestVariableBindings(pdu, false, false, target.getVersion());
    // test it
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    // first try should return local error
    target.setSecurityName(new OctetString("SHAAES"));
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHAAES"), AuthSHA.ID, new OctetString("_12345678_"), null, null));
    try {
      ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
      // This will be hit if engine ID discovery is enabled
      Assert.assertNull(resp.getResponse());
    } catch (MessageException mex) {
      // This will only happen if no engine ID discovery is needed
      Assert.assertEquals(SnmpConstants.SNMPv3_USM_UNSUPPORTED_SECURITY_LEVEL, mex.getSnmp4jErrorStatus());
    }
    // second try without engine ID discovery
    try {
      ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
      // This will be hit if engine ID discovery is enabled
      Assert.assertNull(resp);
    } catch (MessageException mex) {
      // This will only happen if no engine ID discovery is needed
      Assert.assertEquals(SnmpConstants.SNMPv3_USM_UNSUPPORTED_SECURITY_LEVEL, mex.getSnmp4jErrorStatus());
    }
    SNMP4JSettings.setReportSecurityLevelStrategy(SNMP4JSettings.ReportSecurityLevelStrategy.noAuthNoPrivIfNeeded);
    // third try: remote error
    snmpCommandGenerator.getUSM().removeAllUsers(new OctetString("SHAAES"));
    snmpCommandResponder.getUSM().removeAllUsers(new OctetString("SHAAES"));
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHAAES"), AuthSHA.ID, new OctetString("_12345678_"), PrivAES128.ID,
            new OctetString("$secure$")));
    snmpCommandResponder.getUSM().addUser(
        new UsmUser(new OctetString("SHAAES"), AuthSHA.ID, new OctetString("_12345678_"), null, null));
    target.setAuthoritativeEngineID(snmpCommandResponder.getLocalEngineID());
    pdu.setContextEngineID(new OctetString(snmpCommandResponder.getLocalEngineID()));
    ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
    PDU expectedResponse =
        makeReport(pdu, new VariableBinding(SnmpConstants.usmStatsUnsupportedSecLevels, new Counter32(1)));
    // request ID will be 0 because ScopedPDU could not be parsed:
    expectedResponse.setRequestID(new Integer32(0));
    ((ScopedPDU)expectedResponse).setContextEngineID(new OctetString(snmpCommandResponder.getLocalEngineID()));
    Assert.assertEquals(expectedResponse, resp.getResponse());

    // Test standard behavior
    SNMP4JSettings.setReportSecurityLevelStrategy(SNMP4JSettings.ReportSecurityLevelStrategy.standard);
    target.setAuthoritativeEngineID(snmpCommandResponder.getLocalEngineID());
    pdu.setContextEngineID(new OctetString(snmpCommandResponder.getLocalEngineID()));
    resp = snmpCommandGenerator.send(pdu, target);
    // We expect null (timeout) as response, because sender has no matching privacy protocol to return message.
    Assert.assertNull(resp.getResponse());

  }

  @Test
  public void testGetV3_RFC3414_3_2_6() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(1000);
    target.setVersion(SnmpConstants.version3);
    target.setSecurityName(new OctetString("SHADES"));
    target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.GET);
    addTestVariableBindings(pdu, false, false, target.getVersion());
    // test it
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHADES"), AuthSHA.ID, new OctetString("_12345678_"),
            PrivDES.ID, new OctetString("_09876543#1_")));

    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();

    ResponseEvent resp = snmpCommandGenerator.send(pdu, target);
    // no response because receiver cannot decode the message.
    Assert.assertNull(resp.getResponse());

    // next try no authentication, so with standard report strategy we will not receive a report
    snmpCommandGenerator.getUSM().removeAllUsers(new OctetString("SHADES"));
    snmpCommandGenerator.getUSM().addUser(
        new UsmUser(new OctetString("SHADES"), AuthSHA.ID, new OctetString("_12345#78_"),
            PrivDES.ID, new OctetString("_09876543#1_")));
    target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);

    resp = snmpCommandGenerator.send(pdu, target);
    Assert.assertNull(resp.getResponse());

    // same but with relaxed report strategy
    SNMP4JSettings.setReportSecurityLevelStrategy(SNMP4JSettings.ReportSecurityLevelStrategy.noAuthNoPrivIfNeeded);
    resp = snmpCommandGenerator.send(pdu, target);
    // The usmStatsWrongDigests counter was incremented to 3 because we had already two before
    PDU expectedResponse = makeReport(pdu, new VariableBinding(SnmpConstants.usmStatsWrongDigests, new Counter32(3)));
    expectedResponse.setRequestID(new Integer32(0));
    ((ScopedPDU)expectedResponse).setContextEngineID(new OctetString(snmpCommandResponder.getUSM().getLocalEngineID()));
    Assert.assertEquals(expectedResponse, resp.getResponse());
  }

  private void syncRequestTest(Target target, PDU pdu) throws IOException {
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    ResponseEvent resp =
        snmpCommandGenerator.send(pdu, target);
    PDU expectedResponse = makeResponse(pdu, target.getVersion());
    Assert.assertEquals(expectedResponse, resp.getResponse());
  }
  */

  private void asyncRequestTest(Target target, PDU pdu) throws IOException {
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, makeResponse(pdu, target.getVersion())));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    final AsyncResponseListener asyncResponseListener = new AsyncResponseListener(queue.size());
    snmpCommandGenerator.send(pdu, target, null, asyncResponseListener);
    synchronized (asyncResponseListener) {
      try {
        asyncResponseListener.wait(20000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /*
  private void unconfirmedTest(Target target, PDU pdu) throws IOException {
    Map<Integer, RequestResponse> queue = new HashMap<>(2);
    queue.put(pdu.getRequestID().getValue(), new RequestResponse(pdu, null));
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandResponder.listen();
    ResponseEvent resp =
        snmpCommandGenerator.send(pdu, target, transportMappingCG);
    Assert.assertNull(resp);
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException iex) {
      // ignore
    }
    Assert.assertTrue(queue.isEmpty());
  }

  private void unconfirmedTestNullResult(Target target, PDU pdu) throws IOException {
    Map<Integer, RequestResponse> queue = Collections.emptyMap();
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandResponder.listen();
    ResponseEvent resp =
        snmpCommandGenerator.send(pdu, target, transportMappingCG);
    Assert.assertNull(resp);
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException iex) {
      // ignore
    }
    Assert.assertFalse(responder.isAnyResponse());
  }
  */

  private PDU makeResponse(PDU pdu, int version) {
    PDU responsePDU = (PDU) pdu.clone();
    responsePDU.setType(PDU.RESPONSE);
    responsePDU.setErrorStatus(PDU.noError);
    responsePDU.setErrorIndex(0);
    responsePDU.getVariableBindings().clear();
    addTestVariableBindings(responsePDU, true, true, version);
    return responsePDU;
  }

  private PDU makeReport(PDU pdu, VariableBinding reportVariable) {
    PDU responsePDU = (PDU) pdu.clone();
    responsePDU.setType(PDU.REPORT);
    responsePDU.setErrorStatus(PDU.noError);
    responsePDU.setErrorIndex(0);
    responsePDU.getVariableBindings().clear();
    responsePDU.add(reportVariable);
    return responsePDU;
  }

  private void addTestVariableBindings(PDU pdu, boolean withValue, boolean withNull, int version) {
    pdu.add(new VariableBinding(new OID(SnmpConstants.sysDescr), (withValue) ?
        new OctetString("Test string with öä°#+~§ and normal text.1234567890123456789012345678901234567890{}") : Null.instance));
    pdu.add(new VariableBinding(new OID(SnmpConstants.sysObjectID), (withValue) ? new OID("1.3.6.1.4.1.4976") : Null.instance));
    if (version > SnmpConstants.version1) {
      pdu.add(new VariableBinding(new OID("1.1"), (withValue) ? new Counter64(1234567890123456789L) : Null.instance));
    }
    pdu.add(new VariableBinding(new OID("1.2"), (withValue) ? new Integer32(Integer.MAX_VALUE) : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.1"), (withValue) ? new UnsignedInteger32(((long) Integer.MIN_VALUE & 0xFFFFFF)) : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.2"), (withValue) ? new Counter32(Integer.MAX_VALUE * 2L) : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.3"), (withValue) ? new Gauge32(Integer.MAX_VALUE / 2) : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.4"), (withValue) ? new TimeTicks(12345678) : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.5"), (withValue) ? new IpAddress("127.0.0.1") : Null.instance));
    pdu.add(new VariableBinding(new OID("1.3.1.6.6"), (withValue) ? new Opaque(new byte[]{0, -128, 56, 48, 0, 1}) : Null.instance));
    if (withNull) {
      pdu.add(new VariableBinding(new OID("1.3.1.6.7"), (withValue) ? Null.noSuchInstance : Null.instance));
    }
  }

  @Test(timeout = 30000)
  public void testGetNextV3Async() throws InterruptedException, IOException {
    Target target = userTarget;
    target.setTimeout(50000L);
    target.setRetries(0);
    Map<Integer, RequestResponse> queue = new HashMap<>(10000);
    for (int i=0; i<99; i++) {
      ScopedPDU pdu = new ScopedPDU();
      pdu.add(new VariableBinding(new OID("1.3.6.1.4976.1."+i), new Integer32(i)));
      pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
      RequestResponse rr = new RequestResponse(pdu, (PDU)pdu.clone());
      rr.response.setType(PDU.RESPONSE);
      queue.put(pdu.getRequestID().getValue(), rr);
      pdu.get(0).setVariable(Null.instance);

    }
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    int n = 0;
    final AsyncResponseListener asyncResponseListener = new AsyncResponseListener(queue.size());
    List<RequestResponse> requests = new ArrayList<>(queue.values());
    for (RequestResponse rr : requests) {
      snmpCommandGenerator.send(rr.request, target, transportMappingCG, n, asyncResponseListener);
      n++;
//      Thread.sleep(1L);
    }
    synchronized (asyncResponseListener) {
      asyncResponseListener.wait(20000);
    }
  }

  @Test(timeout = 30000)
  public void testGetNextV3AsyncUserChange() throws InterruptedException, IOException {
    Target target = userTarget;
    target.setTimeout(50000L);
    target.setRetries(0);
    Map<Integer, RequestResponse> queue = new HashMap<>(10000);
    for (int i=0; i<999; i++) {
      ScopedPDU pdu = new ScopedPDU();
      pdu.add(new VariableBinding(new OID("1.3.6.1.4976.1."+i), new Integer32(i)));
      pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
      RequestResponse rr = new RequestResponse(pdu, (PDU)pdu.clone());
      rr.response.setType(PDU.RESPONSE);
      queue.put(pdu.getRequestID().getValue(), rr);
      pdu.get(0).setVariable(Null.instance);

    }
    TestCommandResponder responder = new TestCommandResponder(queue);
    snmpCommandResponder.addCommandResponder(responder);
    snmpCommandGenerator.listen();
    snmpCommandResponder.listen();
    int n = 0;
    final AsyncResponseListener asyncResponseListener = new AsyncResponseListener(queue.size());
    List<RequestResponse> requests = new ArrayList<>(queue.values());
    for (RequestResponse rr : requests) {
      snmpCommandGenerator.send(rr.request, target, transportMappingCG, n, asyncResponseListener);
      n++;
//      Thread.sleep(1L);
    }
    synchronized (asyncResponseListener) {
      snmpCommandResponder.getUSM().removeAllUsers(new OctetString("SHADES2"));
      asyncResponseListener.wait(1000);
      snmpCommandResponder.getUSM().addUser(
          new UsmUser(new OctetString("SHADES2"), AuthSHA.ID, new OctetString("_12345678_"),
              PrivDES.ID, new OctetString("_0987654321_")));
    }
  }

  /*
  @Test
  public void testTrapV1() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version1);
    PDUv1 pdu = new PDUv1();
    pdu.setType(PDU.V1TRAP);
    pdu.setAgentAddress(new IpAddress("127.0.0.1"));
    pdu.setEnterprise(new OID("1.3.6.1.4.1.4976"));
    pdu.setSpecificTrap(9);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    unconfirmedTest(target, pdu);
  }

  @Test
  public void testTrapV2WithV1() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version1);
    PDU pdu = new PDU();
    pdu.setType(PDU.NOTIFICATION);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    unconfirmedTestNullResult(target, pdu);
  }

  @Test
  public void testTrapV2WithV1Allowed() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version1);
    PDU pdu = new PDU();
    pdu.setType(PDU.NOTIFICATION);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    SNMP4JSettings.setAllowSNMPv2InV1(true);
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    unconfirmedTest(target, pdu);
  }

  @Test
  public void testNotifyV2c() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version2c);
    PDU pdu = new PDU();
    pdu.setType(PDU.NOTIFICATION);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    unconfirmedTest(target, pdu);
  }

  @Test
  public void testNotifyV3() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(10000);
    target.setVersion(SnmpConstants.version3);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.NOTIFICATION);
    pdu.setContextName(new OctetString("myContext"));
    addTestVariableBindings(pdu, false, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    unconfirmedTest(target, pdu);
  }

  @Test
  public void testInformV2c() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version2c);
    PDU pdu = new PDU();
    pdu.setType(PDU.INFORM);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    syncRequestTest(target, pdu);
  }

  @Test
  public void testInformV3() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(10000);
    target.setVersion(SnmpConstants.version3);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.INFORM);
    pdu.setContextName(new OctetString("myContext"));
    addTestVariableBindings(pdu, false, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    syncRequestTest(target, pdu);
  }
  */

  @Test
  public void testInformV2cAsync() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version2c);
    PDU pdu = new PDU();
    pdu.setType(PDU.INFORM);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    asyncRequestTest(target, pdu);
  }

  @Test
  public void testInformV3Async() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(10000);
    target.setVersion(SnmpConstants.version3);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.INFORM);
    pdu.setContextName(new OctetString("myContext"));
    addTestVariableBindings(pdu, false, false, target.getVersion());
    pdu.setRequestID(new Integer32(snmpCommandGenerator.getNextRequestID()));
    asyncRequestTest(target, pdu);
  }

  /*
  @Test
  public void testSetV1() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version1);
    PDU pdu = new PDU();
    pdu.setType(PDU.SET);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    syncRequestTest(target, pdu);
  }

  @Test
  public void testSetV2c() throws IOException {
    CommunityTarget target = (CommunityTarget) communityTarget.clone();
    target.setVersion(SnmpConstants.version2c);
    PDU pdu = new PDU();
    pdu.setType(PDU.SET);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    syncRequestTest(target, pdu);
  }

  @Test
  public void testSetV3() throws IOException {
    UserTarget target = (UserTarget) userTarget.clone();
    target.setTimeout(10000);
    target.setVersion(SnmpConstants.version3);
    ScopedPDU pdu = new ScopedPDU();
    pdu.setContextName(new OctetString("myContext"));
    pdu.setType(PDU.SET);
    addTestVariableBindings(pdu, true, false, target.getVersion());
    syncRequestTest(target, pdu);
  }
  */

  class TestCommandResponder implements CommandResponder {

    private Map<Integer, RequestResponse> expectedPDUs;
    private boolean anyResponse;

    public TestCommandResponder(PDU request, PDU response) {
      this.expectedPDUs = new HashMap<>(1);
      expectedPDUs.put(request.getRequestID().getValue(), new RequestResponse(request, response));
    }

    public TestCommandResponder(Map<Integer, RequestResponse> expectedPDUs) {
      this.expectedPDUs = expectedPDUs;
    }

    public boolean isAnyResponse() {
      return anyResponse;
    }

    @Override
    public synchronized void processPdu(CommandResponderEvent event) {
      anyResponse = true;
      PDU pdu = event.getPDU();
      if (!expectedPDUs.isEmpty()) {
        Assert.assertNotNull(pdu);
        RequestResponse expected = expectedPDUs.remove(pdu.getRequestID().getValue());
/*
        if (!expected.request.equals(pdu)) {
          System.err.println("DummyTransport: "+transportMappingCG);
          System.err.println(expectedPDUs);
        }  */
        Assert.assertNotNull(expected);
        Assert.assertEquals(expected.request, pdu);
        try {
          // adjust context engine ID after engine ID discovery
          if ((expected != null) && (expected.response != null)) {
            if (expected.request instanceof ScopedPDU) {
              ScopedPDU scopedPDU = (ScopedPDU)expected.request;
              OctetString contextEngineID = scopedPDU.getContextEngineID();
              if ((contextEngineID != null) && (contextEngineID.length()>0)) {
                ((ScopedPDU)expected.response).setContextEngineID(contextEngineID);
              }
            }
            snmpCommandResponder.getMessageDispatcher().returnResponsePdu(
                event.getMessageProcessingModel(), event.getSecurityModel(),
                event.getSecurityName(), event.getSecurityLevel(),
                expected.response, event.getMaxSizeResponsePDU(),
                event.getStateReference(), new StatusInformation());
          }
        } catch (MessageException e) {
          Assert.assertNull(e);
        }
      }
      else {
        Assert.assertNull(pdu);
      }

    }
  }

  class RequestResponse {
    public PDU request;
    public PDU response;

    public RequestResponse(PDU request, PDU response) {
      this.request = request;
      this.response = response;
    }

    @Override
    public String toString() {
      return "RequestResponse{" +
          "request=" + request +
          ", response=" + response +
          '}';
    }
  }

  private class AsyncResponseListener implements ResponseListener {

    private int maxCount = 0;
    private int received = 0;
    private Set<Integer32> receivedIDs = new HashSet<>();

    public AsyncResponseListener(int maxCount) {
      this.maxCount = maxCount;
    }

    @Override
    public synchronized void onResponse(ResponseEvent event) {
      ((Session)event.getSource()).cancel(event.getRequest(), this);
      Assert.assertTrue(receivedIDs.add(event.getRequest().getRequestID()));
      ++received;
      Assert.assertNotNull(event.getResponse());
      Assert.assertNotNull(event.getResponse().get(0));
      Assert.assertNotNull(event.getResponse().get(0).getVariable());
      if (received >= maxCount) {
        notify();
      }
      Assert.assertFalse((received > maxCount));
    }
  }
}
