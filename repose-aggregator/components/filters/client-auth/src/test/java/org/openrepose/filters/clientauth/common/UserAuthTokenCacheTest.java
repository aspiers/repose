/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.filters.clientauth.common;

import org.openrepose.common.auth.AuthToken;
import org.openrepose.core.services.datastore.Datastore;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class UserAuthTokenCacheTest {
   
   public static final String VALID_USER = "user", VALID_AUTH_TOKEN = "abcdef", CACHE_PREFIX = "prefix";

   
   public static class WhenCachingAuthTokens {

      protected AuthTokenCache infoCache;
      protected AuthToken originalUser;
      protected Datastore mockedDatastore;

      @Before
      public void standUp() throws Exception {
         originalUser = mock(AuthToken.class, withSettings().serializable());
         when(originalUser.getUserId()).thenReturn("userId");
         when(originalUser.getUsername()).thenReturn("username");
         when(originalUser.getExpires()).thenReturn(10000l);
         when(originalUser.getRoles()).thenReturn("roles");
         when(originalUser.getTokenId()).thenReturn("token");
         mockedDatastore = mock(Datastore.class);
         
         final String cacheFullName =CACHE_PREFIX + "." + VALID_USER; 
         
         when(mockedDatastore.get(eq(cacheFullName))).thenReturn(originalUser);
         
         infoCache = new AuthTokenCache(mockedDatastore, "prefix") {

            @Override
            public boolean validateToken(AuthToken cachedValue) {
               return true;
            }
         };
      }

      @Test
      public void shouldCorrectlyRetrieveValidCachedUserInfo() {
         final AuthToken user = infoCache.getUserToken(VALID_USER);

         assertEquals("UserId must match original", originalUser.getUserId(), user.getUserId());
         assertEquals("Username must match original", originalUser.getUsername(), user.getUsername());
         assertEquals("Expires must match original", originalUser.getExpires(), user.getExpires());
         assertEquals("Roles must match original", originalUser.getRoles(), user.getRoles());
         assertEquals("TokenId must match original", originalUser.getTokenId(), user.getTokenId());
      }
   }
}
