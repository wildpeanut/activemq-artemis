/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.server.routing.targets;

import javax.security.auth.Subject;

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.core.server.routing.KeyResolver;
import org.apache.activemq.artemis.core.server.routing.KeyType;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
import org.apache.commons.collections.set.ListOrderedSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class KeyTypeResolverTest {

   @Test
   public void testClientIDKey() {
      testClientIDKey("TEST", "TEST", null);
   }

   @Test
   public void testClientIDKeyWithFilter() {
      testClientIDKey("TEST", "TEST1234", "^.{4}");
   }

   private void testClientIDKey(String expected, String clientID, String filter) {
      KeyResolver keyResolver = new KeyResolver(KeyType.CLIENT_ID, filter);

      Assert.assertEquals(expected, keyResolver.resolve(null, clientID, null));

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));
   }

   @Test
   public void testSNIHostKey() {
      testSNIHostKey("TEST", "TEST", null);
   }

   @Test
   public void testSNIHostKeyWithFilter() {
      testSNIHostKey("TEST", "TEST1234", "^.{4}");
   }

   private void testSNIHostKey(String expected, String sniHost, String filter) {
      Connection connection = Mockito.mock(Connection.class);

      KeyResolver keyResolver = new KeyResolver(KeyType.SNI_HOST, filter);

      Mockito.when(connection.getSNIHostName()).thenReturn(sniHost);
      Assert.assertEquals(expected, keyResolver.resolve(connection, null, null));

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));

      Mockito.when(connection.getSNIHostName()).thenReturn(null);
      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));
   }

   @Test
   public void testSourceIPKey() {
      testSourceIPKey("10.0.0.1", "10.0.0.1:12345", null);
   }

   @Test
   public void testSourceIPKeyWithFilter() {
      testSourceIPKey("10", "10.0.0.1:12345", "^[^.]+");
   }

   private void testSourceIPKey(String expected, String remoteAddress, String filter) {
      Connection connection = Mockito.mock(Connection.class);

      KeyResolver keyResolver = new KeyResolver(KeyType.SOURCE_IP, filter);

      Mockito.when(connection.getRemoteAddress()).thenReturn(remoteAddress);
      Assert.assertEquals(expected, keyResolver.resolve(connection, null, null));

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));

      Mockito.when(connection.getRemoteAddress()).thenReturn(null);
      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));
   }

   @Test
   public void testUserNameKey() {
      testUserNameKey("TEST", "TEST", null);
   }

   @Test
   public void testUserNameKeyWithFilter() {
      testUserNameKey("TEST", "TEST1234", "^.{4}");
   }

   private void testUserNameKey(String expected, String username, String filter) {
      KeyResolver keyResolver = new KeyResolver(KeyType.USER_NAME, filter);

      Assert.assertEquals(expected, keyResolver.resolve(null, null, username));

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(null, null, null));
   }

   @Test
   public void testRoleNameKeyWithFilter() throws Exception {
      KeyResolver keyResolver = new KeyResolver(KeyType.ROLE_NAME, "B");

      Connection connection = Mockito.mock(Connection.class);
      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      RemotingConnection protocolConnection = Mockito.mock(RemotingConnection.class);
      Mockito.when(connection.getProtocolConnection()).thenReturn(protocolConnection);
      Subject subject = Mockito.mock(Subject.class);
      Mockito.when(protocolConnection.getAuditSubject()).thenReturn(subject);

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      Set<RolePrincipal> rolePrincipals = new HashSet<>();
      Mockito.when(subject.getPrincipals(RolePrincipal.class)).thenReturn(rolePrincipals);

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      rolePrincipals.add(new RolePrincipal("A"));

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      rolePrincipals.add(new RolePrincipal("B"));

      Assert.assertEquals("B", keyResolver.resolve(connection, null, null));
   }

   @Test
   public void testRoleNameKeyWithoutFilter() throws Exception {
      KeyResolver keyResolver = new KeyResolver(KeyType.ROLE_NAME, null);

      Connection connection = Mockito.mock(Connection.class);
      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      RemotingConnection protocolConnection = Mockito.mock(RemotingConnection.class);
      Mockito.when(connection.getProtocolConnection()).thenReturn(protocolConnection);
      Subject subject = Mockito.mock(Subject.class);
      Mockito.when(protocolConnection.getAuditSubject()).thenReturn(subject);

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      Set<RolePrincipal> rolePrincipals = new ListOrderedSet();
      Mockito.when(subject.getPrincipals(RolePrincipal.class)).thenReturn(rolePrincipals);

      Assert.assertEquals(KeyResolver.DEFAULT_KEY_VALUE, keyResolver.resolve(connection, null, null));

      final RolePrincipal roleA = new RolePrincipal("A");
      rolePrincipals.add(roleA);

      Assert.assertEquals("A", keyResolver.resolve(connection, null, null));

      rolePrincipals.add(new RolePrincipal("B"));

      Assert.assertEquals("A", keyResolver.resolve(connection, null, null));

      rolePrincipals.remove(roleA);
      // with no filter, the first entry matches
      Assert.assertEquals("B", keyResolver.resolve(connection, null, null));
   }
}
