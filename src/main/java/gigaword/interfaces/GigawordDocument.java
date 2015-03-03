/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package gigaword.interfaces;

import gigaword.GigawordDocumentType;

import java.util.List;
import java.util.Optional;

/**
 * Interface that provides a way to describe Gigaword documents. Types are bound
 * by the {@link GigawordDocumentType} enumeration.
 */
public interface GigawordDocument {
  public String getText();
  public String getId();
  public List<TextSpan> getTextSpans();
  public Optional<String> getHeadline();
  public Optional<String> getDateline();
  public long getMillis();
  public GigawordDocumentType getType();
}
