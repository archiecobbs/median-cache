# median-cache

This project is a simple demonstration of one answer to the following question:

**Scenario**: You are maintaining a very large database that includes numerical values. For example, it might be a database of home sale prices. The data set is being constantly updated with additions and removals. You want to be able to efficiently determine the median value at any point in time.

You have defined an index on the data column that sorts by numerical value. So you can efficiently search for any value, and iterate starting from any value in the forward or reverse direction, but you can't (for example) efficiently calculate a value's index in the sorted list, or efficiently jump to an index in the sorted list, or even calcluate the size of the list.

As a result, the straightforward algorithm of retrieving the value(s) at the exact midpoint in that ordering is not a practical solution.

**Question**: Is there a small amount of additional data that you could maintain, updating with each addition or removal, that would allow you to compute the median in constant time?

**Answer**: Almost. Here's an overview.

First, review the definition of median value. Given a sorted list of values:

* If there are zero values, the median is undefined
* If there are an odd number of values, the median is the value in the middle of the list
* If there are an even number of values, the median is the average of the two values in the middle of the list

We define the "first half" and "second half" of the list as follows:

* If the number of values is even, then use the usual/obvious definition of first and second half
* If the number of values is odd, then consider the two halves to overlap by one, so they both include the middle value

So another definition of the median is as the average of:
* The last (i.e., highest) value in the first half, and
* The first (i.e., lowest) value in the second half.

Let's call these values `lo` and `hi` (respectively). When the number of values is odd, these are the same value.

This algorithm keeps track of the following statistics:

* The total number of values
* The last value in the first half (call this `lo`)
* The first value in the second half (call this`hi`)
* The number of duplicates of `lo` that precede it in the first half (call this `dupLo`)
* The number of duplicates of `hi` that follow it in the second half (call this `dupHi`)

When there is an addition or removal, we update these statistics.

**Performance**

Let's get this out of the way first: We are relying on the ability to iterate values in order from any starting point. Initiating such an operation typically takes O(log(n)) time because we have to search into a balanced tree. For this analysis, we are factoring out that search time. So at the end of the day, you would need to multiply all of this analysis by log(n) for it to be accurate.

First we note that updating `dupLo` and `dupHi` may require iterating through an arbitrarily large number of duplicate values.

To see why, suppose we didn't keep track of `dupLo` and `dupHi`, and were tracking a data set where the middle half of values were all the same:
```
                                         lo  hi
    ┏━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┓
    ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃
    ┗━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┛
      00  01  02  03  04  05  06  07  08  09  10  11  12  13  14  15  16  17  18  19
```
Now suppose we remove a `2` from the data set. To confirm that the new `lo` is `2` and not `1`, we would have to iterate through at least four `1`'s or ten `2`'s, and do a similar iteration to confirm the new `hi` is `2` and not `3`. Either way, we're looking at an O(n) operation.

So `dupLo` and `dupHi` just provide a first layer of protection against large duplicate scans, by remembering the results of the last scan performed in each direction. When no duplicate scan is needed, then each operation is constant time.

We could make **every** operation constant time by keeping another table of **unique** values along with their multiplicities, in effect doing the aforementioned remembering of the number of duplicates for every value.

For example, there could be two database tables `SALES` and `SALE_PRICES`, where `SALE_PRICES` had a _unique_ index on the `PRICE` column and also a `NUM_DUPS` column, and each row of `SALES` had a foreign key referring to `SALE_PRICES`.

Another observation is that when there are duplicates, the pattern of additions and removals matters. Consider this data set:
```
                                        lo+hi
    ┏━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┓
    ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 2 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃
    ┗━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┛
      00  01  02  03  04  05  06  07  08  09  10  11  12  13  14  15  16  17  18
```
Now suppose we repeatedly add and remove `2`. Then each operation will require a scan of half of the data set.

At the opposite extreme, if we only did additions, or only removals, then even without keeping track of duplicates the running time is amoritzed constant time, because each duplicate value is scanned at most once.

So the bottom line is that for guaranteed constant time performance in the face of additions and removals, you'd need to either not have duplicates, or else put all the unique values in a separate table where the number of duplicates for any value could be calculated in constant time.

**Demo**

To run the demo/test program:
```shell
$ javac *.java
$ java -ea MedianTest
```
