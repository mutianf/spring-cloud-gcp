/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.autoconfigure.bigtable;

import com.google.cloud.spring.core.Credentials;
import com.google.cloud.spring.core.CredentialsSupplier;
import com.google.cloud.spring.core.GcpScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for configuring Cloud Bigtable.
 */
@ConfigurationProperties("spring.cloud.gcp.bigtable")
public class GcpBigtableProperties implements CredentialsSupplier {

  /** Overrides the GCP OAuth2 credentials specified in the Core module. */
  @NestedConfigurationProperty
  private final Credentials credentials =
      new Credentials(
          "https://www.googleapis.com/auth/bigtable.data",
          "https://www.googleapis.com/auth/bigtable.admin.table",
          GcpScope.CLOUD_PLATFORM.getUrl());

  /** Properties to auto-configure a local Bigtable emulator. */
  @NestedConfigurationProperty
  private final Emulator emulator = new Emulator();

  /** Overrides the GCP project ID specified in the Core module to use for Cloud Bigtable. */
  private String projectId;

  /** The Cloud Bigtable instance ID. */
  private String instanceId;

  /** The Cloud Bigtable app profile ID. */
  private String appProfileId;

  /** The host and port of a Cloud Bigtable emulator (e.g. localhost:8086). */
  private String emulatorHost;

  @Override
  public Credentials getCredentials() {
    return this.credentials;
  }

  public Emulator getEmulator() {
    return this.emulator;
  }

  public String getProjectId() {
    return this.projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getAppProfileId() {
    return this.appProfileId;
  }

  public void setAppProfileId(String appProfileId) {
    this.appProfileId = appProfileId;
  }

  public String getEmulatorHost() {
    if (this.emulatorHost != null) {
      return this.emulatorHost;
    }
    if (this.emulator.isEnabled()) {
      return this.emulator.getHost() + ":" + this.emulator.getPort();
    }
    return null;
  }

  public void setEmulatorHost(String emulatorHost) {
    this.emulatorHost = emulatorHost;
  }

  /**
   * Settings for configuring the Cloud Bigtable emulator.
   */
  public static class Emulator {

    private boolean enabled;
    private String host = "localhost";
    private int port = 8086;

    public boolean isEnabled() {
      return this.enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getHost() {
      return this.host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return this.port;
    }

    public void setPort(int port) {
      this.port = port;
    }
  }
}
