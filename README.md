# median-cache

This project is an answer to the following question:

**Scenario**: You are maintaining a very large database that includes numerical values. For example, it might be a database of home sale prices. The data set is being continually updated with additions and removals. You want to be able to efficiently determine the median value at any point in time.

You can assume there is a sorted index on the column of numerical values. So you can efficiently search for any value and iterate consecutive values starting from any value in the forward or reverse direction, but the data set is too large to efficiently calculate a value's index in the sorted list, jump to an particular index in the sorted list, or count the size of the list.

As a result, the straightforward algorithm of building a list of all of the values and retrieving the value(s) in the middle of the list is impractical.

**Question**: Is there a small amount of additional data that you could maintain that would allow you to, at any time, peform either of the following two operations efficiently?

* Update the median after an addition or removal
* Compute the median

**Answer**: Yes

First, let's define "efficiently". We will be relying on the ability to iterate values in order from any starting point. Initiating such an operation typically takes O(log(n)) time because we have to search into a balanced tree. Also, adding or removing values requires time O(log(n)) to update the column index for the same reason. So the best we can expect to do is O(log(n)) time and that will be our standard for "efficient".

First, let's review the definition of **median value**. Given a sorted list of values:

* If there are zero values, the median is undefined
* If there are an odd number of values, the median is the value in the middle of the list
* If there are an even number of values, the median is the average of the two values in the middle of the list

Now define the **first half** and **second half** of a list as follows:

* If the number of values is even, then use the usual/obvious definition of first and second half
* If the number of values is odd, then consider the two halves to overlap on that one value, so both halves include the middle value

So another definition of the median is the average of:
* The last (i.e., highest) value in the first half, and
* The first (i.e., lowest) value in the second half.

Let's call these values `lo` and `hi` (respectively). When the number of values is odd, these are the same value. The above definition has the property that it works for both odd and even sized lists.

Our algorithm will keep track of the following statistics as the "additional data":

* The total number of values
* The last value in the first half (i.e., `lo`)
* The first value in the second half (i.e., `hi`)

When there is an addition or removal, we update these statistics.

**Performance**

First, let's assume there are no duplicate values. Then updating `lo` and `hi` after an insertion or removal is easy: just determine which half of the list contains the affected value and update `lo` and `hi` accordingly, which at worst requires just finding its next-door neighbor, which we can do in O(log(n)) time.

What if there are duplicates? Suppose we are tracking a data set where the middle half of the values were all the same:
```
                                         lo  hi
    ┏━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┓
    ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 2 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃
    ┗━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┛
      00  01  02  03  04  05  06  07  08  09  10  11  12  13  14  15  16  17  18  19
```
Now suppose we remove a `2` from the data set. `lo` and `hi` are currently both `2` and now we need to update them. The only efficient access we have into the list of values is to iterate forward or backward starting from some particular value; and the only possible starting points for any such iteration are `00`, `04`, `05`, `14`, `15`, and `19`. To prove that `lo` is still `2`, i.e., there is at least one `2` in the first half of the list, we would have to iterate through at least five `1`'s or ten `2`'s. Those are both O(n) operations.

Another observation is that when there are duplicates, the pattern of additions and removals matters. Consider this data set:
```
                                        lo+hi
    ┏━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┳━━━┓
    ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 1 ┃ 2 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃ 3 ┃
    ┗━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┻━━━┛
      00  01  02  03  04  05  06  07  08  09  10  11  12  13  14  15  16  17  18
```
Now suppose we repeatedly add and remove `2`. Then _every_ operation would require a scan of half of the data set.

We can try to handle the problem of duplicates by also tracking the following information:

* The number of duplicates of `lo` that precede it in the first half (call this `dupLo`)
* The number of duplicates of `hi` that follow it in the second half (call this `dupHi`)

In other words, we remember the results of the last duplicate scan performed in each direction. But when `dupLo` or `dupHi` are zero, we may have to do a new scan. So tracking `dupLo` and `dupHi` provides a first layer of defense against large duplicate scans, but the original problem still exists once you get to the next value, and so we still have O(n) performance.

On the other hand, if we could assume there are no duplicate values, then we don't need to track `dupLo` or `dupHi` and every operation is O(log(n)) time.

There is a simple way to elimimate duplicate values: create a new table of **unique** values, along with their multiplicities. In other words we are keeping track of the number of duplicates of **every** value.

For example, there could be two database tables `HOME_SALES` and `SALE_PRICES`, where `SALE_PRICES` had a _unique_ index on the `PRICE` column and also a `NUM_DUPS` column. Then the `HOME_SALES` table would not have a `PRICE` column, but instead would have a foreign key into `SALE_PRICES`. Maintaining this new table only requires O(log(n)) time per addition or removal in `HOME_SALES`.

So the bottom line is that for guaranteed O(log(n)) time performance in the face of additions and removals, you'd need to either assume there are no duplicates, or else maintain a separate table of unique values.

The sample code here includes the `dupLo` and `dupHi` trick. If there are no duplicates, it has guaranteed O(log(n)) performance, and if there are duplicates, at least you have a first layer of defense against bad performance.

**Demo**

To run the demo/test program:
```shell
$ javac *.java
$ java -ea MedianTest
```
