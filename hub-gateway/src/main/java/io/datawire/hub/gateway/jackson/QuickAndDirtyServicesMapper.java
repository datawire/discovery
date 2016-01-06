package io.datawire.hub.gateway.jackson;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class QuickAndDirtyServicesMapper {

  private final ObjectMapper mapper;

  public QuickAndDirtyServicesMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Map<String, Object> toMap(String data) throws Exception {
    return mapper.readValue(data, new TypeReference<Map<String, Object>>() {});
  }
}
