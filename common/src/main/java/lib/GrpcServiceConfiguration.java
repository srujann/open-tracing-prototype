package lib;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

public class GrpcServiceConfiguration {
  @NotEmpty
  private String wavefrontReporterMechanism = "proxy";

  @NotEmpty
  private String host = "localhost";

  @NotEmpty
  private String port = "2878";

  private String wavefrontServer;
  private String token;

  @NotEmpty
  private String application = "defaultApplication";

  private String cluster;

  @NotEmpty
  private String service = "defaultService";

  private String shard;

  private Map<String, String> tags;

  @JsonProperty
  public String getWavefrontReporterMechanism() {
    return wavefrontReporterMechanism;
  }

  @JsonProperty
  public String getHost() {
    return host;
  }

  @JsonProperty
  public String getPort() {
    return port;
  }

  @JsonProperty
  public String getWavefrontServer() {
    return wavefrontServer;
  }

  @JsonProperty
  public String getToken() {
    return token;
  }

  @JsonProperty
  public String getApplication() {
    return application;
  }

  @JsonProperty
  public String getCluster() {
    return cluster;
  }

  @JsonProperty
  public String getService() {
    return service;
  }

  @JsonProperty
  public String getShard() {
    return shard;
  }

  @JsonProperty
  public Map<String, String> getTags() {
    return tags;
  }
}
