import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Keeps track of the median of a set of numbers.
 *
 * <p>
 * Instances require the following two things:
 * <ul>
 *  <li>Notification of every addition and removal of a data value.
 *  <li>The ability to iterate through the data set forward and backwards starting from a given value.
 * </ul>
 *
 * <p>
 * The above information must be consistent with there being an actual set of data. In other words, if there
 * is a remove event for a value that was never added, or if the iterators return data that is impossible
 * given previous events and/or iterations, then the behavior is undefined.
 *
 * <p>
 * Only finite values are supported.
 */
public class MedianState {

    private final DoubleFunction<DoubleStream> downwardIterator;
    private final DoubleFunction<DoubleStream> upwardIterator;

    private long count;         // the total number of values
    private double lo;          // the value at index indexLo() (0.0 when empty)
    private double hi;          // the value at index indexHi() (0.0 when empty)
    private long dupLo;         // the number of duplicate values equal to "lo" having index < indexLo()
    private long dupHi;         // the number of duplicate values equal to "hi" having index > indexHi()

    /**
     * Construct an empty instance.
     *
     * <p>
     * The given functions must accept finite values, {@link Double#POSITIVE_INFINITY}, and {@link Double#NEGATIVE_INFINITY}.
     * The returned {@link DoubleStream}s must contain only finite values.
     *
     * @param downwardIterator iterates values strictly lower than the value given in descending order
     * @param upwardIterator iterates values strictly greater than the value given in asccending order
     * @throws IllegalArgumentException if either parameter is null
     */
    public MedianState(DoubleFunction<DoubleStream> downwardIterator, DoubleFunction<DoubleStream> upwardIterator) {
        if (downwardIterator == null)
            throw new IllegalArgumentException("null downwardIterator");
        if (upwardIterator == null)
            throw new IllegalArgumentException("null upwardIterator");
        this.downwardIterator = downwardIterator;
        this.upwardIterator = upwardIterator;
    }

    /**
     * Get the current median.
     *
     * @return current median
     * @throws IllegalStateException if there is no data
     */
    public double median() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return this.odd() ? this.lo : (this.lo + this.hi) / 2.0;
    }

    /**
     * Get the number of values in the data set.
     *
     * @return number of values
     */
    public long size() {
        return this.count;
    }

    /**
     * Determine if the data set is empty.
     *
     * @return number of values
     */
    public boolean isEmpty() {
        return this.count == 0;
    }

    /**
     * Update the median state after adding a new value to the data set.
     *
     * <p>
     * The iterators provided to the constructor must reflect the state of the data set <i>after</i> the given
     * value has been added.
     *
     * @param value new value
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public void afterAdd(double value) {
        if (!Double.isFinite(value))
            throw new IllegalArgumentException("invalid value");

        // Update median state
        if (this.count == 0) {
            this.lo = value;
            this.hi = value;
            assert this.dupLo == 0;
            assert this.dupHi == 0;
        } else if (this.odd()) {                // odd count
            final double midIndex = this.indexLo();
            final double midValue = this.lo;
            if (value < midValue) {
                if (this.dupLo == 0)
                    this.recalculateLo(midValue);
                else
                    this.dupLo--;
            } else if (value > midValue) {
                if (this.dupHi == 0)
                    this.recalculateHi(midValue);
                else
                    this.dupHi--;
            }
        } else {                                // even count
            if (this.lo == this.hi) {
                final double midValue = this.lo;
                if (value <= midValue)
                    this.dupHi++;
                if (value >= midValue)
                    this.dupLo++;
            } else if (value <= this.lo) {
                this.hi = this.lo;
                this.dupHi = 0;
                if (value == this.lo)
                    this.dupLo++;
            } else if (value >= this.hi) {
                this.lo = this.hi;
                this.dupLo = 0;
                if (value == this.hi)
                    this.dupHi++;
            } else {                            // value is between lo & hi
                this.lo = this.hi = value;
                this.dupLo = 0;
                this.dupHi = 0;
            }
        }
        this.count++;
    }

    /**
     * Update the median state after removing a value from the data set.
     *
     * <p>
     * The iterators provided to the constructor must reflect the state of the data set <i>after</i> the given
     * value has been removed.
     *
     * <p>
     * If the given value is not actually present in the data set, then the behavior is undefined.
     *
     * @param value old value
     * @throws IllegalStateException if there is no data
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public void afterRemove(double value) {

        // Update median state
        if (this.count == 0)
            throw new IllegalStateException("no data");
        if (this.count == 1) {
            this.lo = 0.0;
            this.hi = 0.0;
            assert this.dupLo == 0;
            assert this.dupHi == 0;
        } else if (this.odd()) {                       // odd count
            final double midValue = this.lo;
            final double midIndex = this.indexLo();
            if (value <= midValue) {
                if (this.dupHi > 0)
                    this.dupHi--;
                else
                    this.recalculateHi(midValue);
            }
            if (value >= midValue) {
                if (this.dupLo > 0)
                    this.dupLo--;
                else
                    this.recalculateLo(midValue);
            }
        } else {                                // even count
            if (this.lo == this.hi) {
                final double midValues = this.lo;
                if (value < midValues)
                    this.dupLo++;
                else if (value > midValues)
                    this.dupHi++;
            } else if (value <= this.lo) {
                this.lo = this.hi;
                this.dupLo = 0;
            } else {
                assert value >= this.hi;
                this.hi = this.lo;
                this.dupHi = 0;
            }
        }
        this.count--;
    }

    private boolean odd() {
        return (this.count & 1) != 0;
    }

    /**
     * Get the index of the last value in the first half of values.
     *
     * @return index of "lo"
     * @throws IllegalStateException if there is no data
     */
    public long indexLo() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return (this.count + 1) / 2 - 1;
    }

    /**
     * Get the index of the first value in the second half of values.
     *
     * @return index of "hi"
     * @throws IllegalStateException if there is no data
     */
    public long indexHi() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return (this.count / 2 - 1) + 1;
    }

    /**
     * Get the last value in the first half of values.
     *
     * @return index of "lo"
     * @throws IllegalStateException if there is no data
     */
    public double lo() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return this.lo;
    }

    /**
     * Get the first value in the second half of values.
     *
     * @return index of "hi"
     * @throws IllegalStateException if there is no data
     */
    public double hi() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return this.hi;
    }

    /**
     * Get the number of duplicates of "lo" in the first half of values.
     *
     * @return number of duplicates of "lo", or zero if there is no data
     */
    public long dupLo() {
        return this.dupLo;
    }

    /**
     * Get the number of duplicates of "hi" in the second half of values.
     *
     * @return number of duplicates of "hi", or zero if there is no data
     */
    public long dupHi() {
        return this.dupHi;
    }

    /**
     * Confirm that our notion of the median agrees with what's actually in the data set.
     *
     * <p>
     * Note: This performs an O(n) iteration of the entire data set.
     *
     * @throws IllegalStateException if there is a discrepancy
     */
    public void verify() {

        // First, determine whether Double.MIN_VALUE is in the data set. We have to do
        // this separately because the upwardIterator only iterates strictly greater values.
        final boolean hasMinValue;
        try (DoubleStream stream = this.downwardIterator.apply(Double.MIN_VALUE + Double.MIN_VALUE)) {
            hasMinValue = stream.findAny().isPresent();
        }

        // We will watch for "lo" and "hi" as we scan past them
        final long actualIndexLo = (this.count + 1) / 2 - 1;
        final long actualIndexHi = (this.count / 2 - 1) + 1;
        double actualLo = 0.0;
        double actualHi = 0.0;

        // Scan through all the values
        long index = 0;
        try (DoubleStream stream = this.upwardIterator.apply(Double.NEGATIVE_INFINITY)) {
            for (Iterator<Double> i = stream.iterator(); i.hasNext(); ) {
                final double value = i.next();
                if (index == this.count)
                    throw new IllegalStateException("there are more than " + this.count + " values");
                assert Double.isFinite(value);
                if (index == actualIndexLo)
                    actualLo = value;
                if (index == actualIndexHi)
                    actualHi = value;
                index++;
            }
        }
        if (index != this.count)
            throw new IllegalStateException("there are only " + index + " < " + this.count + " values");

        // Verify
        if (index > 0) {
            final double actualMedian = (actualLo + actualHi) / 2.0;
            if (actualMedian != this.median())
                throw new IllegalStateException("incorrect median " + this.median() + " != " + actualMedian);
        }
    }

    private void recalculateLo(double value) {
        try (DoubleStream stream = this.downwardIterator.apply(value)) {
            final Iterator<Double> i = stream.iterator();
            assert i.hasNext();
            this.lo = i.next();
            this.dupLo = 0;
            while (i.hasNext() && i.next().equals(this.lo))
                this.dupLo++;
        }
    }

    private void recalculateHi(double value) {
        try (DoubleStream stream = this.upwardIterator.apply(value)) {
            final Iterator<Double> i = stream.iterator();
            assert i.hasNext();
            this.hi = i.next();
            this.dupHi = 0;
            while (i.hasNext() && i.next().equals(this.hi))
                this.dupHi++;
        }
    }

// Object

    @Override
    public String toString() {
        if (this.count == 0)
            return "MedianState[empty]";
        return String.format(
          "MedianState[count=%d,lo=%.1f@%d+%d,hi=%.1f@%d+%d]",
          this.count, this.lo, this.indexLo(), this.dupLo, this.hi, this.indexHi(), this.dupHi);
    }
}
