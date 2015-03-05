/*
    Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.gson.stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * {@link JsonReader} impementation backed by Jackson's {@link JsonParser}.
 * Provides measurable JSON parsing improvements over Gson's native implementation.
 * Error reporting might differ, however.
 */
@NotThreadSafe
public class JsonParserReader extends JsonReader implements Callable<JsonParser> {

  private static final Reader UNSUPPORTED_READER = new Reader() {
    @Override
    public int read(char[] buffer, int offset, int count) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException();
    }
  };

  private final JsonParser parser;

  public JsonParserReader(JsonParser parser) {
    super(UNSUPPORTED_READER);
    this.parser = parser;
  }

  public JsonParser getParser() {
    return parser;
  }

  @Nullable
  private com.fasterxml.jackson.core.JsonToken peek;

  private void clearPeek() {
    peek = null;
  }

  private void requirePeek() throws IOException {
    if (peek == null) {
      peek = parser.nextToken();
    }
  }

  @Override
  public void beginArray() throws IOException {
    requirePeek();
    expect(START_ARRAY);
    clearPeek();
  }

  @Override
  public void endArray() throws IOException {
    requirePeek();
    expect(END_ARRAY);
    clearPeek();
  }

  @Override
  public void beginObject() throws IOException {
    requirePeek();
    expect(START_OBJECT);
    clearPeek();
  }

  @Override
  public void endObject() throws IOException {
    requirePeek();
    expect(END_OBJECT);
    clearPeek();
  }

  @Override
  public boolean hasNext() throws IOException {
    requirePeek();
    com.fasterxml.jackson.core.JsonToken token = peek;
    return token != END_OBJECT & token != END_ARRAY;
  }

  @Override
  public JsonToken peek() throws IOException {
    requirePeek();
    return toGsonToken(peek);
  }

  private void expect(com.fasterxml.jackson.core.JsonToken expected) {
    if (peek != expected) {
      throw new IllegalStateException("Expected " + expected + " but was " + peek);
    }
  }

  @Override
  public String nextName() throws IOException {
    requirePeek();
    expect(FIELD_NAME);
    String name = parser.getText();
    clearPeek();
    return name;
  }

  @Override
  public String nextString() throws IOException {
    requirePeek();
    if (!isLenient()) {
      expect(VALUE_STRING);
    }
    String value = parser.getText();
    clearPeek();
    return value;
  }

  @Override
  public boolean nextBoolean() throws IOException {
    requirePeek();
    boolean value = parser.getBooleanValue();
    clearPeek();
    return value;
  }

  @Override
  public void nextNull() throws IOException {
    requirePeek();
    expect(VALUE_NULL);
    clearPeek();
  }

  @Override
  public double nextDouble() throws IOException {
    requirePeek();
    double value = parser.getDoubleValue();
    clearPeek();
    return value;

  }

  @Override
  public long nextLong() throws IOException {
    requirePeek();
    long value = parser.getLongValue();
    clearPeek();
    return value;
  }

  @Override
  public int nextInt() throws IOException {
    requirePeek();
    int value = parser.getIntValue();
    clearPeek();
    return value;
  }

  @Override
  public void close() throws IOException {
    clearPeek();
    parser.close();
  }

  @Override
  public void skipValue() throws IOException {
    requirePeek();
    parser.skipChildren();
    clearPeek();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + parser.getCurrentLocation() + ")";
  }

  public void promoteNameToValue() {
    throw new UnsupportedOperationException();
  }

  private static JsonToken toGsonToken(com.fasterxml.jackson.core.JsonToken token) {
    switch (token) {
    case START_ARRAY:
      return JsonToken.BEGIN_ARRAY;
    case END_ARRAY:
      return JsonToken.END_ARRAY;
    case START_OBJECT:
      return JsonToken.BEGIN_OBJECT;
    case END_OBJECT:
      return JsonToken.END_OBJECT;
    case FIELD_NAME:
      return JsonToken.NAME;
    case VALUE_FALSE:
      return JsonToken.BOOLEAN;
    case VALUE_TRUE:
      return JsonToken.BOOLEAN;
    case VALUE_NULL:
      return JsonToken.NULL;
    case VALUE_NUMBER_INT:
      return JsonToken.NUMBER;
    case VALUE_NUMBER_FLOAT:
      return JsonToken.NUMBER;
    case VALUE_STRING:
      return JsonToken.STRING;
    default: // Not semantically equivalent
      return JsonToken.NULL;
    }
  }

  /**
   * Implements {@link Callable} mostly as a marker interface.
   * Better use {@link #getParser()} to get parser.
   * @return unwrapped {@link JsonParser}
   */
  @Override
  public JsonParser call() throws Exception {
    return parser;
  }

  /**
   * Reads current value including objects and array as effiecient token buffer.
   * Use of Jackson's own mechanisms is important to preserve custom elements
   * such as special embedded objects in BSON or other data formats.
   * @return {@link TokenBuffer}
   * @throws IOException if error occured
   */
  public final TokenBuffer nextTokenBuffer() throws IOException {
    TokenBuffer buffer = new TokenBuffer(parser);
    // if token is consumed, but undelying parser is still sitting on this token, we move forward
    requirePeek();
    buffer.copyCurrentStructure(parser);
    // when we will return to reading from reader, state will be cleared and nextToken after
    clearPeek();
    return buffer;
  }

  protected final void consumePeek() {
    clearPeek();
  }
}
