/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.exceptions;

/**
 * An application or API error that occurs when a system tries to communicate with an external API,
 * third-party server, or downstream service, but the request fails.
 */
public class ExternalServiceException extends RuntimeException {
  public ExternalServiceException(String message) {
    super(message);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
