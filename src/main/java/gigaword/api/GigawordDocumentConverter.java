/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package gigaword.api;

import gigaword.interfaces.GigawordDocument;

import java.nio.file.Path;
import java.util.Iterator;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.LazySeq;

/**
 * Java wrapper around Clojure Gigaword API.
 * <br>
 * <br>
 * Provides a way to stream over Gigaword documents, an ability to convert
 * single Gigaword .sgml documents, and an ability to convert strings that
 * represent .sgml documents.
 */
public class GigawordDocumentConverter {

  private IFn gzPathToGigaDocIterFx;
  private IFn pathToGigaDocFx;
  private IFn gigaDocStrToGigaDocFx;
  private IFn gzPathToGigaStringIterFx;

  /**
   * Default, no-arg ctor.
   */
  public GigawordDocumentConverter() {
    IFn req = Clojure.var("clojure.core", "require");
    req.invoke(Clojure.read("gigaword.core"));
    req.invoke(Clojure.read("gigaword.interop"));

    this.gzPathToGigaDocIterFx = Clojure.var("gigaword.interop", "gigazip->pcs");
    this.pathToGigaDocFx = Clojure.var("gigaword.interop", "proxydoc->pc");
    this.gigaDocStrToGigaDocFx = Clojure.var("gigaword.interop", "proxystr->pc");
    this.gzPathToGigaStringIterFx = Clojure.var("gigaword.core", "gigazip->proxystrs");
  }

  /**
   * @param pathToGigaSGMLFile a string that represents a path to a .sgml file on disk.
   * @return a {@link GigawordDocument} object that represents the .sgml file
   */
  public GigawordDocument fromPathString(String pathToGigaSGMLFile) {
    return (GigawordDocument)this.pathToGigaDocFx.invoke(pathToGigaSGMLFile);
  }

  /**
   * @param gigaDocSGMLString a string that represents the contents of a .sgml file
   * @return a {@link GigawordDocument} object that represents the string
   */
  public GigawordDocument fromSGMLString(String gigaDocSGMLString) {
    return (GigawordDocument)this.gigaDocStrToGigaDocFx.invoke(gigaDocSGMLString);
  }

  /**
   * @param pathToGZFile a path to a .gz file of concatenated .sgml files
   * @return an {@link Iterator} of {@link GigawordDocument} objects from the .gz file
   */
  public Iterator<GigawordDocument> iterator(String pathToGZFile) {
    LazySeq seq = (LazySeq)this.gzPathToGigaDocIterFx.invoke(pathToGZFile);
    return new GigawordDocumentIterator(seq.iterator());
  }

  @SuppressWarnings("unchecked")
  public Iterator<String> stringIterator(Path pathToGZFile) {
    LazySeq seq = (LazySeq)this.gzPathToGigaStringIterFx.invoke(pathToGZFile.toString());
    return (Iterator<String>)seq.iterator();
  }

  /**
   * Java iterator that allows streaming of {@link GigawordDocument} objects.
   */
  private class GigawordDocumentIterator implements Iterator<GigawordDocument> {
    private final Iterator<?> iter;

    private GigawordDocumentIterator(Iterator<?> iter) {
      this.iter = iter;
    }

    public boolean hasNext() {
      return this.iter.hasNext();
    }

    public GigawordDocument next() {
      return (GigawordDocument) this.iter.next();
    }
  }
}
