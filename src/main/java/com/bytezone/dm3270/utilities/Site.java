package com.bytezone.dm3270.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Site {

  private static final Logger LOG = LoggerFactory.getLogger(Site.class);

  public final String name;
  public final boolean extended;
  private final String url;
  private int port;

  public Site(String name, String url, int port, boolean extended) {
    this.name = name;
    this.url = url;
    this.port = port;
    this.extended = extended;
  }

  public String getName() {
    return name;
  }

  public String getURL() {
    return url;
  }

  public int getPort() {
    if (port <= 0) {
      LOG.warn("Invalid port value: {}. Fallback to default value {}", port, 23);
      port = 23;
    }
    return port;
  }

  public boolean getExtended() {
    return extended;
  }

  @Override
  public String toString() {
    return String.format("Site [name=%s, url=%s, port=%d]", getName(), getURL(), getPort());
  }

}
