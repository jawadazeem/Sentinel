/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.exceptions;

public class NodeRecordNotFoundException extends RuntimeException {
  public NodeRecordNotFoundException(String message) {
    super(message);
  }
}
