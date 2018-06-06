package org.folio.edge.core.utils;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class Mappers {
  public static final String XML_PROLOG = "<?xml version='1.0' encoding='UTF-8'?>\n";

  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  public static final ObjectMapper jsonMapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setDateFormat(new SimpleDateFormat(DATE_FORMAT));

  public static final XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setDateFormat(new SimpleDateFormat(DATE_FORMAT));

  private Mappers() {

  }
}
