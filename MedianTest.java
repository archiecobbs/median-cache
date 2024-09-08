import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class MedianTest {

    private final int maxSize;
    private final int maxValue;
    private final Random random = new Random();
    private final double[] values;              // the actual values
    private int size;

    public MedianTest(int maxSize, int maxValue) {
        if (maxSize <= 0)
            throw new IllegalArgumentException("invalid maxSize");
        if (maxValue <= 0)
            throw new IllegalArgumentException("invalid maxValue");
        this.maxSize = maxSize;
        this.maxValue = maxValue;
        this.values = new double[this.maxSize];
    }

    public void run(int maxIterations) {

        // Iterators used by MedianState - this is its only view into the actual data
        final DoubleFunction<DoubleStream> downwardIterator = value -> {
            final int upperBound;
            if (value == Double.NEGATIVE_INFINITY)
                return DoubleStream.empty();
            if (value == Double.POSITIVE_INFINITY)
                upperBound = this.size;
            else {
                if (!Double.isFinite(value))
                    throw new IllegalArgumentException("invalid value");
                int index = Arrays.binarySearch(this.values, 0, this.size, value);
                if (index >= 0) {
                    while (index > 0 && this.values[index - 1] == value)            // skip backwards over values equal to "value"
                        index--;
                    upperBound = index;
                } else
                    upperBound = ~index;
            }
            return IntStream.range(0, upperBound)
              .map(i -> upperBound - i - 1)             // iterate in descending order
              .mapToDouble(i -> this.values[i]);
        };
        final DoubleFunction<DoubleStream> upwardIterator = value -> {
            final int lowerBound;
            if (value == Double.POSITIVE_INFINITY)
                return DoubleStream.empty();
            if (value == Double.NEGATIVE_INFINITY)
                lowerBound = 0;
            else {
                int index = Arrays.binarySearch(this.values, 0, this.size, value);
                if (index >= 0) {
                    while (index < this.size - 1 && this.values[index] == value)    // skip forward over values equal to "value"
                        index++;
                    lowerBound = index;
                } else
                    lowerBound = ~index;
            }
            return IntStream.range(lowerBound, this.size)
              .mapToDouble(i -> this.values[i]);
        };

        // Median state
        final MedianState state = new MedianState(downwardIterator, upwardIterator);

        // Run test
        for (int iterations = 0; iterations < maxIterations; iterations++) {

            // Check invariants
            assert this.size >= 0 && this.size <= this.values.length;
            for (int i = 0; i < this.size; i++) {
                assert Double.isFinite(this.values[i]);
                assert this.values[i] >= 0 && this.values[i] < this.maxValue;
                if (i > 0)
                    assert this.values[i] >= this.values[i - 1];
            }
            if (this.size == 0) {
                assert state.lo() == 0;
                assert state.hi() == 0;
                assert state.dupLo() == 0;
                assert state.dupLo() == 0;
            } else {
                assert state.lo() == this.values[(int)state.indexLo()];
                assert state.hi() == this.values[(int)state.indexHi()];
                assert state.hi() >= state.lo();
                assert 1 + state.dupLo() == IntStream.range(0, (int)state.indexLo() + 1)
                  .filter(i -> this.values[i] == state.lo())
                  .count();
                assert 1 + state.dupHi() == IntStream.range((int)state.indexHi(), this.size)
                  .filter(i -> this.values[i] == state.hi())
                  .count();
            }
            state.verify();

            // Mutate state
            while (true) {

                // Try adding a new value
                if (this.size < this.maxSize && random.nextBoolean()) {
                    final double newValue = random.nextInt(this.maxValue);
                    int index = Arrays.binarySearch(this.values, 0, this.size, newValue);
                    if (index < 0)
                        index = ~index;
                    System.out.println(String.format("ADD: %02d @ %02d", (int)newValue, index));
                    System.arraycopy(this.values, index, this.values, index + 1, this.size++ - index);
                    this.values[index] = newValue;
                    state.afterAdd(newValue);
                    break;
                }

                // Try removing an old value
                if (this.size > 0 && random.nextBoolean()) {
                    final int index = random.nextInt(this.size);
                    final double oldValue = this.values[index];
                    System.out.println(String.format("DEL: %02d @ %02d", (int)oldValue, index));
                    System.arraycopy(this.values, index + 1, this.values, index, --this.size - index);
                    state.afterRemove(oldValue);
                    break;
                }
            }

            // Print state
            System.out.println("New state: " + state);
            for (int i = 0; i < this.size; i++) {
                final double value = this.values[i];
                char beforeChar;
                if (i == state.indexLo())
                    beforeChar = '<';
                else if (i == state.indexHi())
                    beforeChar = '|';
                else if (i - 1 == state.indexHi())
                    beforeChar = '>';
                else
                    beforeChar = ' ';
                System.out.print(String.format("%c%02d", beforeChar, (int)this.values[i]));
            }
            if (this.size > 0 && this.values[this.size - 1] == state.hi())
                System.out.print(']');
            System.out.println();
        }
    }

    public static void main(String[] args) throws Exception {

        // Initialiez defaults
        int maxSize = 50;
        int maxValue = 30;
        int maxIterations = Integer.MAX_VALUE;

        // Initialize test
        final MedianTest medianTest;
        try {
            switch (args.length) {
            case 3:
                maxIterations = Integer.parseInt(args[2], 10);
                // FALLTHROUGH
            case 2:
                maxValue = Integer.parseInt(args[1], 10);
                // FALLTHROUGH
            case 1:
                maxSize = Integer.parseInt(args[0], 10);
                // FALLTHROUGH
            case 0:
                break;
            default:
                throw new IllegalArgumentException();
            }
            medianTest = new MedianTest(maxSize, maxValue);
        } catch (Exception e) {
            System.out.println("Usage: MedianTest [ maxSize [ maxValue [ maxIterations ] ] ]");
            System.exit(1);
            throw new RuntimeException();
        }

        // Run test
        medianTest.run(maxIterations);
    }
}
