package org.folio.edge.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@RunWith(VertxUnitRunner.class)
public class InstitutionalUserHelperTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Test
  public void testWithoutCache(TestContext context) {
    var secureStore = mock(SecureStore.class);
    when(secureStore.get(any(), any(), any(), eq("name"))).thenReturn(Future.succeededFuture("pass"));
    var okapiClient = mock(OkapiClient.class);
    when(okapiClient.doLogin(eq("name"), eq("pass"))).thenReturn(Future.succeededFuture("tok"));
    var institutionalUserHelper = new InstitutionalUserHelper(secureStore);
    institutionalUserHelper.fetchToken(okapiClient, null, null, "name")
    .onComplete(context.asyncAssertSuccess(result -> assertThat(result, is("tok"))));
  }

}
