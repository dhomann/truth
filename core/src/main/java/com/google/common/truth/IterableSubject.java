/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.truth;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.SubjectUtils.accumulate;
import static com.google.common.truth.SubjectUtils.countDuplicates;
import static java.util.Arrays.asList;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Propositions for {@link Iterable} subjects.
 *
 * @author Kurt Alfred Kluever
 */
public class IterableSubject<S extends IterableSubject<S, T, C>, T, C extends Iterable<T>>
    extends Subject<S, C> {

  @SuppressWarnings({ "unchecked", "rawtypes" })
  static <T, C extends Iterable<T>> IterableSubject<? extends IterableSubject<?, T, C>, T, C>
      create(FailureStrategy failureStrategy, Iterable<T> list) {
    return new IterableSubject(failureStrategy, list);
  }

  IterableSubject(FailureStrategy failureStrategy, C list) {
    super(failureStrategy, list);
  }

  /**
   * Fails if the subject is not empty.
   */
  public void isEmpty() {
    if (!Iterables.isEmpty(getSubject())) {
      fail("is empty");
    }
  }

  /**
   * Fails if the subject is empty.
   */
  public void isNotEmpty() {
    if (Iterables.isEmpty(getSubject())) {
      // TODO(user): "Not true that <[]> is not empty" doesn't really need the <[]>,
      // since it's empty. But would the bulkier "the subject" really be better?
      // At best, we could *replace* <[]> with a given label (rather than supplementing it).
      // Perhaps the right failure message is just "<[]> should not have been empty"
      fail("is not empty");
    }
  }

  /**
   * Fails if the subject does not have the given size.
   */
  public final void hasSize(int expectedSize) {
    checkArgument(expectedSize >= 0, "expectedSize(%s) must be >= 0", expectedSize);
    int actualSize = Iterables.size(getSubject());
    if (actualSize != expectedSize) {
      failWithBadResults("has a size of", expectedSize, "is", actualSize);
    }
  }

  /**
   * Asserts that the items are supplied in the order given by the iterable. If
   * the iterable under test and/or the {@code expectedItems} do not provide
   * iteration order guarantees (say, {@link Set}{@code <?>}s), this method may provide
   * unexpected results.  Consider using {@link #isEqualTo(Object)} in such cases, or using
   * collections and iterables that provide strong order guarantees.
   */
  // TODO(user): @deprecated Use {@code containsExactlyElementsIn(Iterable).inOrder()} instead.
  public void iteratesAs(Iterable<?> expectedItems) {
    Iterator<T> actualItems = getSubject().iterator();
    for (Object expected : expectedItems) {
      if (!actualItems.hasNext()) {
        fail("iterates through", expectedItems);
      } else {
        Object actual = actualItems.next();
        if (actual == expected || actual != null && actual.equals(expected)) {
          continue;
        } else {
          fail("iterates through", expectedItems);
        }
      }
    }
    if (actualItems.hasNext()) {
      fail("iterates through", expectedItems);
    }
  }

  /**
   * @deprecated Use {@code containsExactly(Object, Object...).inOrder()} instead.
   */
  @Deprecated
  public void iteratesOverSequence(Object... expectedItems) {
    iteratesAs(expectedItems);
  }

  /**
   * Asserts that the items are supplied in the order given by the iterable. If
   * the iterable under test does not provide iteration order guarantees (say,
   * a {@link Set}{@code <?>}), this method is not suitable for asserting that order.
   * Consider using {@link #isEqualTo(Object)}
   */
  // TODO(user): @deprecated Use {@code containsExactly(Object, Object...).inOrder()} instead.
  public void iteratesAs(Object... expectedItems) {
    iteratesAs(Arrays.asList(expectedItems));
  }

  /**
   * Attests (with a side-effect failure) that the subject contains the
   * supplied item.
   */
  public void contains(@Nullable Object element) {
    if (!Iterables.contains(getSubject(), element)) {
      failWithRawMessage("%s should have contained <%s>", getDisplaySubject(), element);
    }
  }

  /**
   * Attests (with a side-effect failure) that the subject does not contain
   * the supplied item.
   */
  public void doesNotContain(@Nullable Object element) {
    if (Iterables.contains(getSubject(), element)) {
      failWithRawMessage("%s should not have contained <%s>", getDisplaySubject(), element);
    }
  }

  /**
   * Attests that the subject does not contain duplicate elements.
   */
  public void containsNoDuplicates() {
    List<Entry<T>> duplicates = Lists.newArrayList();
    for (Multiset.Entry<T> entry : LinkedHashMultiset.create(getSubject()).entrySet()) {
      if (entry.getCount() > 1) {
        duplicates.add(entry);
      }
    }
    if (!duplicates.isEmpty()) {
      failWithRawMessage("%s has the following duplicates: <%s>", getDisplaySubject(), duplicates);
    }
  }

  /**
   * Attests that the subject contains at least one of the provided objects
   * or fails.
   */
  public void containsAnyOf(@Nullable Object first, @Nullable Object second, Object... rest) {
    contains("contains any of", accumulate(first, second, rest));
  }

  /**
   * Attests that a Collection contains at least one of the objects contained
   * in the provided collection or fails.
   */
  public void containsAnyIn(Iterable<?> expected) {
    contains("contains any element in", expected);
  }

  private void contains(String failVerb, Iterable<?> expected) {
    for (Object item : expected) {
      if (Iterables.contains(getSubject(), item)) {
        return;
      }
    }
    fail(failVerb, expected);
  }

  /**
   * Attests that the subject contains at least all of the provided objects
   * or fails, potentially permitting duplicates in both the subject and the
   * parameters (if the subject even can have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   */
  public Ordered containsAllOf(@Nullable Object first, @Nullable Object second, Object... rest) {
    return containsAll("contains all of", accumulate(first, second, rest));
  }

  /**
   * Attests that the subject contains at least all of the provided objects
   * or fails, potentially permitting duplicates in both the subject and the
   * parameters (if the subject even can have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   */
  public Ordered containsAllIn(Iterable<?> expected) {
    return containsAll("contains all elements in", expected);
  }

  private Ordered containsAll(String failVerb, Iterable<?> expected) {
    // would really like to use ArrayDeque here, but it doesn't allow nulls
    List<?> toRemove = Lists.newArrayList(expected);

    boolean inOrder = true;

    // remove each item in the subject, as many times as it occurs in the subject.
    for (Object item : getSubject()) {
      int index = toRemove.indexOf(item);
      if (index != -1) {
        toRemove.remove(index);

        // in order as long as the next expected item in the subject is the first item remaining
        // in toRemove
        inOrder &= (index == 0);
      }
    }

    if (!toRemove.isEmpty()) {
      failWithBadResults(failVerb, expected, "is missing", countDuplicates(toRemove));
    }

    return inOrder ? IN_ORDER : new NotInOrder("contains all elements in order", expected);
  }

  /**
   * Attests that a subject contains all of the provided objects and
   * only these objects or fails, potentially permitting duplicates
   * in both the subject and the parameters (if the subject even can
   * have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   *
   * @deprecated Use {@link #containsExactly(Object...)} instead.
   */
  @Deprecated
  public Ordered containsOnlyElements(
      @Nullable Object first, @Nullable Object second, Object... rest) {
    return containsExactlyElementsIn(accumulate(first, second, rest));
  }

  /**
   * Attests that a subject contains all of the provided objects and
   * only these objects or fails, potentially permitting duplicates
   * in both the subject and the parameters (if the subject even can
   * have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   *
   * @deprecated Use {@link #containsExactlyElementsIn(Iterable)} instead.
   */
  @Deprecated
  public Ordered containsOnlyElementsIn(Iterable<?> expected) {
    return containsExactlyElementsIn(expected);
  }

  /**
   * Attests that a subject contains all of the provided objects and
   * only these objects or fails, potentially permitting duplicates
   * in both the subject and the parameters (if the subject even can
   * have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   */
  public Ordered containsExactly(@Nullable Object... varargs) {
    return containsExactly("contains exactly", asList(varargs));
  }

  /**
   * Attests that a subject contains all of the provided objects and
   * only these objects or fails, potentially permitting duplicates
   * in both the subject and the parameters (if the subject even can
   * have duplicates).
   *
   * <p>Callers may optionally chain an {@code inOrder()} call if its expected
   * contents must be contained in the given order.
   */
  public Ordered containsExactlyElementsIn(Iterable<?> expected) {
    return containsExactly("contains exactly", expected);
  }

  private Ordered containsExactly(String failVerb, Iterable<?> required) {
    Iterator<?> actualIter = getSubject().iterator();
    Iterator<?> requiredIter = required.iterator();

    // Step through both iterators comparing elements pairwise.
    while (actualIter.hasNext() && requiredIter.hasNext()) {
      Object actualElement = actualIter.next();
      Object requiredElement = requiredIter.next();

      // As soon as we encounter a pair of elements that differ, we know that inOrder()
      // cannot succeed, so we can check the rest of the elements more normally.
      // Since any previous pairs of elements we iterated over were equal, they have no
      // effect on the result now.
      if (!Objects.equal(actualElement, requiredElement)) {
        // Missing elements; elements that are not missing will be removed as we iterate.
        Collection<Object> missing = Lists.newArrayList();
        missing.add(requiredElement);
        Iterators.addAll(missing, requiredIter);

        // Extra elements that the subject had but shouldn't have.
        Collection<Object> extra = Lists.newArrayList();

        // Remove all actual elements from missing, and add any that weren't in missing
        // to extra.
        if (!missing.remove(actualElement)) {
          extra.add(actualElement);
        }
        while (actualIter.hasNext()) {
          Object item = actualIter.next();
          if (!missing.remove(item)) {
            extra.add(item);
          }
        }

        // Fail if there are either missing or extra elements.

        // TODO(user): Possible enhancement: Include "[1 copy]" if the element does appear in
        // the subject but not enough times. Similarly for unexpected extra items.
        if (!missing.isEmpty()) {
          failWithBadResults(failVerb, required, "is missing", countDuplicates(missing));
        }
        if (!extra.isEmpty()) {
          failWithBadResults(failVerb, required, "has unexpected items", countDuplicates(extra));
        }

        // Since we know the iterables were not in the same order, inOrder() can just fail.
        return new NotInOrder("contains only these elements in order", required);
      }
    }

    // Here,  we must have reached the end of one of the iterators without finding any
    // pairs of elements that differ. If the actual iterator still has elements, they're
    // extras. If the required iterator has elements, they're missing elements.
    if (actualIter.hasNext()) {
      failWithBadResults(failVerb, required, "has unexpected items",
          countDuplicates(Lists.newArrayList(actualIter)));
    } else if (requiredIter.hasNext()) {
      failWithBadResults(failVerb, required, "is missing",
          countDuplicates(Lists.newArrayList(requiredIter)));
    }

    // If neither iterator has elements, we reached the end and the elements were in
    // order, so inOrder() can just succeed.
    return IN_ORDER;
  }

  /**
   * Attests that a subject contains none of the provided objects
   * or fails, eliding duplicates.
   */
  public void containsNoneOf(@Nullable Object first, @Nullable Object second, Object... rest) {
    containsNone("contains none of", accumulate(first, second, rest));
  }

  /**
   * Attests that a Collection contains none of the objects contained
   * in the provided collection or fails, eliding duplicates.
   */
  public void containsNoneIn(Iterable<?> excluded) {
    containsNone("contains no elements in", excluded);
  }

  private void containsNone(String failVerb, Iterable<?> excluded) {
    Collection<Object> present = new ArrayList<Object>();
    for (Object item : Sets.newHashSet(excluded)) {
      if (Iterables.contains(getSubject(), item)) {
        present.add(item);
      }
    }
    if (!present.isEmpty()) {
      failWithBadResults(failVerb, excluded, "contains", present);
    }
  }

  /**
   * Ordered implementation that always fails.
   */
  private class NotInOrder implements Ordered {
    private final String check;
    private final Iterable<?> required;

    NotInOrder(String check, Iterable<?> required) {
      this.check = check;
      this.required = required;
    }

    @Override public void inOrder() {
      fail(check, required);
    }
  }

  /**
   * Ordered implementation that does nothing because it's already known to be true.
   */
  private static final Ordered IN_ORDER = new Ordered() {
    @Override public void inOrder() {}
  };
}
