/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.fixture.style;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * Feature combination
 * <ul>
 * <li>Nested Builder using constructor "new" invokation
 * </ul>
 */
@Value.Enclosing
@Value.Style(
    builder = "new",
    visibility = ImplementationVisibility.PRIVATE)
public abstract class EnclosingBuilderNew {
  @Value.Immutable
  public static class Hidden {}

  void use() {
    new ImmutableEnclosingBuilderNew.HiddenBuilder().build();
  }
}
