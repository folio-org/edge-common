package org.folio.edge.core.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import io.vertx.core.json.JsonObject;

public class MappersTest {

  @Test
  public void testJsonMapper() throws Exception {
    String key = "foo";
    String value = "bar";

    JsonObject json = new JsonObject();
    json.put(key, value);

    Map<?, ?> asObj = Mappers.jsonMapper.readValue(json.encode(), HashMap.class);

    assertEquals(value, asObj.get(key));
    assertEquals(json.encodePrettily(), Mappers.jsonMapper.writeValueAsString(asObj));
  }

  @Test
  public void testXmlMapper() throws Exception {
    String a = "foo";
    String b = "bar";

    StringBuilder sb = new StringBuilder();
    sb.append(Mappers.XML_PROLOG)
      .append("<test>\n")
      .append("  <a>").append(a).append("</a>\n")
      .append("  <b>").append(b).append("</b>\n")
      .append("</test>\n");

    TestObject obj = new TestObject(a, b);

    String asStr = Mappers.xmlMapper.writeValueAsString(obj);
    assertEquals(sb.toString(), Mappers.XML_PROLOG + asStr);

    TestObject asObj = Mappers.xmlMapper.readValue(asStr, TestObject.class);
    assertEquals(obj.a, asObj.a);
    assertEquals(obj.b, asObj.b);
  }

  @JacksonXmlRootElement(localName = "test")
  public static class TestObject {

    @JacksonXmlProperty(localName = "a")
    private String a;
    @JacksonXmlProperty(localName = "b")
    private String b;

    public TestObject(@JacksonXmlProperty(localName = "a") String a, @JacksonXmlProperty(localName = "b") String b) {
      this.a = a;
      this.b = b;
    }
  }

}
