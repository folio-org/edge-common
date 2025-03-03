package org.folio.edge.core.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import java.util.Properties;

public class AwsParamStore extends SecureStore {

  protected static final Logger logger = LogManager.getLogger(AwsParamStore.class);

  public static final String TYPE = "AwsSsm";

  public static final String PROP_REGION = "region";
  public static final String PROP_USE_IAM = "useIAM";
  public static final String PROP_ECS_CREDENTIALS_PATH = "ecsCredentialsPath";
  public static final String PROP_ECS_CREDENTIALS_ENDPOINT = "ecsCredentialsEndpoint";
  public static final String PROP_AWS_CONTAINER_CREDENTIALS_RELATIVE_URI =
      SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.property();
  public static final String ENV_AWS_CONTAINER_CREDENTIALS_RELATIVE_URI =
      SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.toString();

  public static final String DEFAULT_USE_IAM = "true";

  private String region;
  private boolean useIAM;

  protected SsmClient ssm;

  public AwsParamStore(Properties properties) {
    super(properties);
    logger.info("Initializing...");

    if (properties != null) {
      region = properties.getProperty(PROP_REGION);
      useIAM = Boolean.parseBoolean(properties.getProperty(PROP_USE_IAM, DEFAULT_USE_IAM));
    }

    SsmClientBuilder builder = SsmClient.builder();

    if (region != null) {
      builder.region(Region.of(region));
    }

    if (useIAM) {
      logger.info("Using IAM");
    } else {
      var credProvider = getAwsCredentialsProvider();
      builder.credentialsProvider(credProvider);
    }

    ssm = builder.build();
  }

  private AwsCredentialsProvider getAwsCredentialsProvider() {
    try {
      var credProvider = EnvironmentVariableCredentialsProvider.create();
      credProvider.resolveCredentials();
      logger.info("Using EnvironmentVariableCredentialsProvider");
      return credProvider;
    } catch (Exception e) {
      // ignore, try next
    }
    try {
      var credProvider = SystemPropertyCredentialsProvider.create();
      credProvider.resolveCredentials();
      logger.info("Using SystemPropertyCredentialsProvider");
      return credProvider;
    } catch (Exception e) {
      // ignore, try next
    }
    logger.info("Using ContainerCredentialsProvider");
    var credProvider = ContainerCredentialsProvider.builder().endpoint(endpoint()).build();
    credProvider.resolveCredentials();
    return credProvider;
  }

  private String endpoint() {
    if (properties == null) {
      return null;
    }

    var endpoint = properties.getProperty(PROP_ECS_CREDENTIALS_ENDPOINT);
    if (endpoint == null) {
      return null;
    }

    var path = properties.getProperty(PROP_ECS_CREDENTIALS_PATH);
    if (path != null) {
      System.setProperty(PROP_AWS_CONTAINER_CREDENTIALS_RELATIVE_URI, path);
    }

    return endpoint;
  }

  @Override
  public String get(String clientId, String tenant, String username) throws NotFoundException {
    String key = String.format("%s_%s_%s", clientId, tenant, username);
    GetParameterRequest req = GetParameterRequest.builder()
      .name(key)
      .withDecryption(true)
      .build();

    try {
      return ssm.getParameter(req).parameter().value();
    } catch (ParameterNotFoundException e) {
      throw new NotFoundException(e);
    }
  }

  public String getRegion() {
    return region;
  }

  public boolean getUseIAM() {
    return useIAM;
  }

}
