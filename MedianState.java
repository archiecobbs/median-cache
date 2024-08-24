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
     * @throws IllegalStateException if no data has been added
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
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public void afterRemove(double value) {

        // Update median state
        if (this.count == 1) {
            this.lo = 0.0;
            this.hi = 0.0;
            assert this.dupLo == 0;
            assert this.dupHi == 0;
        } else if (this.odd()) {                // odd count
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

    public long indexLo() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return (this.count + 1) / 2 - 1;
    }

    public long indexHi() {
        if (this.count == 0)
            throw new IllegalStateException("no data");
        return (this.count / 2 - 1) + 1;
    }

    public double lo() {
        return this.lo;
    }

    public double hi() {
        return this.hi;
    }

    public long dupLo() {
        return this.dupLo;
    }

    public long dupHi() {
        return this.dupHi;
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
