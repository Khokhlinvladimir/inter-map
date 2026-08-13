/*
 * HumanTime.java
 *
 * Created on 06.10.2008
 *
 * Copyright (c) 2008 Johann Burkard (<mailto:jb@eaio.com>) <http://eaio.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.osmdroid.debug.util

import java.io.Externalizable
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.math.abs

/**
 * HumanTime parses and formats time deltas for easier reading by humans. It can format time information without losing
 * information but its main purpose is to generate more easily understood approximations.
 * <h3>Using HumanTime</h3>
 *
 *
 * Use HumanTime by creating an instance that contains the time delta ([HumanTime.HumanTime]), create an
 * empty instance through ([HumanTime.HumanTime]) and set the delta using the [.y], [.d],
 * [.h], [.s] and [.ms] methods or parse a [CharSequence] representation ([.eval]).
 * Parsing ignores whitespace and is case insensitive.
 *
 * <h3>HumanTime format</h3>
 *
 *
 * HumanTime will format time deltas in years ("y"), days ("d"), hours ("h"), minutes ("m"), seconds ("s") and
 * milliseconds ("ms"), separated by a blank character. For approximate representations, the time delta will be round up
 * or down if necessary.
 *
 * <h3>HumanTime examples</h3>
 *
 *  * HumanTime.eval("1 d 1d 2m 3m").getExactly() = "2 d 5 m"
 *  * HumanTime.eval("2m8d2h4m").getExactly() = "8 d 2 h 6 m"
 *  * HumanTime.approximately("2 d 8 h 20 m 50 s") = "2 d 8 h"
 *  * HumanTime.approximately("55m") = "1 h"
 *
 * <h3>Implementation details</h3>
 *
 *  * The time delta can only be increased.
 *  * Instances of this class are thread safe.
 *  * Getters using the Java Beans naming conventions are provided for use in environments like JSP or with expression
 * languages like OGNL. See [.getApproximately] and [.getExactly].
 *  * To keep things simple, a year consists of 365 days.
 *
 *
 * @author [Johann Burkard](mailto:jb@eaio.com)
 * @version $Id: HumanTime.java 3906 2011-05-21 13:56:05Z johann $
 * @see .eval
 * @see .approximately
 * @see [Date Formatting and Parsing for Humans in Java with HumanTime](http://johannburkard.de/blog/programming/java/date-formatting-parsing-humans-humantime.html)
 */
class HumanTime @JvmOverloads constructor(delta: Long = 0L) : Externalizable, Comparable<HumanTime>, Cloneable {
    /**
     * Parsing state.
     */
    enum class State {
        NUMBER, IGNORED, UNIT
    }

    /**
     * Returns the time delta.
     *
     * @return the time delta
     */
    /**
     * The time delta.
     */
    var delta: Long
        private set

    /**
     * Constructor for HumanTime.
     *
     * @param delta the initial time delta, interpreted as a positive number
     */
    /**
     * No-argument Constructor for HumanTime.
     *
     *
     * Equivalent to calling `new HumanTime(0L)`.
     */
    init {
        this.delta = abs(delta)
    }

    private fun nTimes(unit: String?, n: Int) {
        if ("ms".equals(unit, ignoreCase = true)) {
            ms(n)
        } else if ("s".equals(unit, ignoreCase = true)) {
            s(n)
        } else if ("m".equals(unit, ignoreCase = true)) {
            m(n)
        } else if ("h".equals(unit, ignoreCase = true)) {
            h(n)
        } else if ("d".equals(unit, ignoreCase = true)) {
            d(n)
        } else if ("y".equals(unit, ignoreCase = true)) {
            y(n)
        }
    }

    private fun upperCeiling(x: Long): Long {
        return (x / 100) * (100 - CEILING_PERCENTAGE)
    }

    private fun lowerCeiling(x: Long): Long {
        return (x / 100) * CEILING_PERCENTAGE
    }

    private fun ceil(d: Long, n: Long): String {
        return kotlin.math.ceil(d.toDouble() / n).toInt().toString()
    }

    private fun floor(d: Long, n: Long): String {
        return kotlin.math.floor(d.toDouble() / n).toInt().toString()
    }

    /**
     * Adds n years to the time delta.
     *
     * @param n n
     * @return this HumanTime object
     */
    /**
     * Adds one year to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun y(n: Int = 1): HumanTime {
        delta += YEAR * abs(n)
        return this
    }

    /**
     * Adds n days to the time delta.
     *
     * @param n n
     * @return this HumanTime object
     */
    /**
     * Adds one day to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun d(n: Int = 1): HumanTime {
        delta += DAY * abs(n)
        return this
    }

    /**
     * Adds n hours to the time delta.
     *
     * @param n n
     * @return this HumanTime object
     */
    /**
     * Adds one hour to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun h(n: Int = 1): HumanTime {
        delta += HOUR * abs(n)
        return this
    }

    /**
     * Adds n months to the time delta.
     *
     * @param n n
     * @return this HumanTime object
     */
    /**
     * Adds one month to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun m(n: Int = 1): HumanTime {
        delta += MINUTE * abs(n)
        return this
    }

    /**
     * Adds n seconds to the time delta.
     *
     * @param n seconds
     * @return this HumanTime object
     */
    /**
     * Adds one second to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun s(n: Int = 1): HumanTime {
        delta += SECOND * abs(n)
        return this
    }

    /**
     * Adds n milliseconds to the time delta.
     *
     * @param n n
     * @return this HumanTime object
     */
    /**
     * Adds one millisecond to the time delta.
     *
     * @return this HumanTime object
     */
    @JvmOverloads
    fun ms(n: Int = 1): HumanTime {
        delta += abs(n).toLong()
        return this
    }

    val exactly: String
        /**
         * Returns a human-formatted representation of the time delta.
         *
         * @return a formatted representation of the time delta, never `null`
         */
        get() = getExactly<StringBuilder?>(StringBuilder()).toString()

    /**
     * Appends a human-formatted representation of the time delta to the given [Appendable] object.
     *
     * @param <T> the return type
     * @param a   the Appendable object, may not be `null`
     * @return the given Appendable object, never `null`
    </T> */
    fun <T : Appendable?> getExactly(a: T?): T? {
        try {
            var prependBlank = false
            var d = delta
            if (d >= YEAR) {
                a!!.append(floor(d, YEAR))
                a.append(' ')
                a.append('y')
                prependBlank = true
            }
            d %= YEAR
            if (d >= DAY) {
                if (prependBlank) {
                    a!!.append(' ')
                }
                a!!.append(floor(d, DAY))
                a.append(' ')
                a.append('d')
                prependBlank = true
            }
            d %= DAY
            if (d >= HOUR) {
                if (prependBlank) {
                    a!!.append(' ')
                }
                a!!.append(floor(d, HOUR))
                a.append(' ')
                a.append('h')
                prependBlank = true
            }
            d %= HOUR
            if (d >= MINUTE) {
                if (prependBlank) {
                    a!!.append(' ')
                }
                a!!.append(floor(d, MINUTE))
                a.append(' ')
                a.append('m')
                prependBlank = true
            }
            d %= MINUTE
            if (d >= SECOND) {
                if (prependBlank) {
                    a!!.append(' ')
                }
                a!!.append(floor(d, SECOND))
                a.append(' ')
                a.append('s')
                prependBlank = true
            }
            d %= SECOND
            if (d > 0) {
                if (prependBlank) {
                    a!!.append(' ')
                }
                a!!.append(d.toInt().toString())
                a.append(' ')
                a.append('m')
                a.append('s')
            }
        } catch (ex: IOException) {
            // What were they thinking...
        }
        return a
    }

    val approximately: String
        /**
         * Returns an approximate, human-formatted representation of the time delta.
         *
         * @return a formatted representation of the time delta, never `null`
         */
        get() = getApproximately<StringBuilder?>(StringBuilder()).toString()

    /**
     * Appends an approximate, human-formatted representation of the time delta to the given [Appendable] object.
     *
     * @param <T> the return type
     * @param a   the Appendable object, may not be `null`
     * @return the given Appendable object, never `null`
    </T> */
    fun <T : Appendable?> getApproximately(a: T?): T? {
        try {
            var parts = 0
            var rounded = false
            var prependBlank = false
            var d = delta
            var mod: Long = d % YEAR

            if (mod >= upperCeiling(YEAR)) {
                a!!.append(ceil(d, YEAR))
                a.append(' ')
                a.append('y')
                ++parts
                rounded = true
                prependBlank = true
            } else if (d >= YEAR) {
                a!!.append(floor(d, YEAR))
                a.append(' ')
                a.append('y')
                ++parts
                rounded = mod <= lowerCeiling(YEAR)
                prependBlank = true
            }

            if (!rounded) {
                d %= YEAR
                mod = d % DAY

                if (mod >= upperCeiling(DAY)) {
                    if (prependBlank) {
                        a!!.append(' ')
                    }
                    a!!.append(ceil(d, DAY))
                    a.append(' ')
                    a.append('d')
                    ++parts
                    rounded = true
                    prependBlank = true
                } else if (d >= DAY) {
                    if (prependBlank) {
                        a!!.append(' ')
                    }
                    a!!.append(floor(d, DAY))
                    a.append(' ')
                    a.append('d')
                    ++parts
                    rounded = mod <= lowerCeiling(DAY)
                    prependBlank = true
                }

                if (parts < 2) {
                    d %= DAY
                    mod = d % HOUR

                    if (mod >= upperCeiling(HOUR)) {
                        if (prependBlank) {
                            a!!.append(' ')
                        }
                        a!!.append(ceil(d, HOUR))
                        a.append(' ')
                        a.append('h')
                        ++parts
                        rounded = true
                        prependBlank = true
                    } else if (d >= HOUR && !rounded) {
                        if (prependBlank) {
                            a!!.append(' ')
                        }
                        a!!.append(floor(d, HOUR))
                        a.append(' ')
                        a.append('h')
                        ++parts
                        rounded = mod <= lowerCeiling(HOUR)
                        prependBlank = true
                    }

                    if (parts < 2) {
                        d %= HOUR
                        mod = d % MINUTE

                        if (mod >= upperCeiling(MINUTE)) {
                            if (prependBlank) {
                                a!!.append(' ')
                            }
                            a!!.append(ceil(d, MINUTE))
                            a.append(' ')
                            a.append('m')
                            ++parts
                            rounded = true
                            prependBlank = true
                        } else if (d >= MINUTE && !rounded) {
                            if (prependBlank) {
                                a!!.append(' ')
                            }
                            a!!.append(floor(d, MINUTE))
                            a.append(' ')
                            a.append('m')
                            ++parts
                            rounded = mod <= lowerCeiling(MINUTE)
                            prependBlank = true
                        }

                        if (parts < 2) {
                            d %= MINUTE
                            mod = d % SECOND

                            if (mod >= upperCeiling(SECOND)) {
                                if (prependBlank) {
                                    a!!.append(' ')
                                }
                                a!!.append(ceil(d, SECOND))
                                a.append(' ')
                                a.append('s')
                                ++parts
                                rounded = true
                                prependBlank = true
                            } else if (d >= SECOND && !rounded) {
                                if (prependBlank) {
                                    a!!.append(' ')
                                }
                                a!!.append(floor(d, SECOND))
                                a.append(' ')
                                a.append('s')
                                ++parts
                                rounded = mod <= lowerCeiling(SECOND)
                                prependBlank = true
                            }

                            if (parts < 2) {
                                d %= SECOND

                                if (d > 0 && !rounded) {
                                    if (prependBlank) {
                                        a!!.append(' ')
                                    }
                                    a!!.append(d.toInt().toString())
                                    a.append(' ')
                                    a.append('m')
                                    a.append('s')
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            // What were they thinking...
        }

        return a
    }

    /**
     * @see Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is HumanTime) {
            return false
        }
        return delta == obj.delta
    }

    /**
     * Returns a 32-bit representation of the time delta.
     *
     * @see Object.hashCode
     */
    override fun hashCode(): Int {
        return (delta xor (delta shr 32)).toInt()
    }

    /**
     * Returns a String representation of this.
     *
     *
     * The output is identical to [.getExactly].
     *
     * @return a String, never `null`
     * @see Object.toString
     * @see .getExactly
     */
    override fun toString(): String {
        return this.exactly
    }

    /**
     * Compares this HumanTime to another HumanTime.
     *
     * @param t the other instance, may not be `null`
     * @return which one is greater
     */
    override fun compareTo(t: HumanTime): Int {
        return if (delta == t.delta) 0 else (if (delta < t.delta) -1 else 1)
    }

    /**
     * Deep-clones this object.
     *
     * @throws CloneNotSupportedException
     * @see Object.clone
     */
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    /**
     * @see Externalizable.readExternal
     */
    @Throws(IOException::class)
    override fun readExternal(`in`: ObjectInput) {
        delta = `in`.readLong()
    }

    /**
     * @see Externalizable.writeExternal
     */
    @Throws(IOException::class)
    override fun writeExternal(out: ObjectOutput) {
        out.writeLong(delta)
    }

    companion object {
        /**
         * The serial version UID.
         */
        private const val serialVersionUID = 5179328390732826722L

        /**
         * One second.
         */
        private const val SECOND: Long = 1000

        /**
         * One minute.
         */
        private val MINUTE: Long = SECOND * 60

        /**
         * One hour.
         */
        private val HOUR: Long = MINUTE * 60

        /**
         * One day.
         */
        private val DAY: Long = HOUR * 24

        /**
         * One year.
         */
        private val YEAR: Long = DAY * 365

        /**
         * Percentage of what is round up or down.
         */
        private const val CEILING_PERCENTAGE = 15

        fun getState(c: Char): State {
            val out: State
            when (c) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> out = State.NUMBER
                's', 'm', 'h', 'd', 'y', 'S', 'M', 'H', 'D', 'Y' -> out = State.UNIT
                else -> out = State.IGNORED
            }
            return out
        }

        /**
         * Parses a [CharSequence] argument and returns a [HumanTime] instance.
         *
         * @param s the char sequence, may not be `null`
         * @return an instance, never `null`
         */
        fun eval(s: CharSequence): HumanTime {
            val out = HumanTime(0L)

            var num = 0

            var start = 0
            var end = 0

            var oldState: State? = State.IGNORED

            for (c in object : Iterable<Char?> {
                /**
                 * @see java.lang.Iterable.iterator
                 */
                override fun iterator(): MutableIterator<Char?> {
                    return object : MutableIterator<Char?> {
                        private var p = 0

                        /**
                         * @see java.util.Iterator.hasNext
                         */
                        override fun hasNext(): Boolean {
                            return p < s.length
                        }

                        /**
                         * @see java.util.Iterator.next
                         */
                        override fun next(): Char {
                            return s.get(p++)
                        }

                        /**
                         * @see java.util.Iterator.remove
                         */
                        override fun remove() {
                            throw UnsupportedOperationException()
                        }
                    }
                }
            }) {
                val newState: State = Companion.getState(c!!)
                if (oldState != newState) {
                    if (oldState == State.NUMBER && (newState == State.IGNORED || newState == State.UNIT)) {
                        num = s.subSequence(start, end).toString().toInt()
                    } else if (oldState == State.UNIT && (newState == State.IGNORED || newState == State.NUMBER)) {
                        out.nTimes(s.subSequence(start, end).toString(), num)
                        num = 0
                    }
                    start = end
                }
                ++end
                oldState = newState
            }
            if (oldState == State.UNIT) {
                out.nTimes(s.subSequence(start, end).toString(), num)
            }

            return out
        }

        /**
         * Parses and formats the given char sequence, preserving all data.
         *
         *
         * Equivalent to `eval(in).getExactly()`
         *
         * @param in the char sequence, may not be `null`
         * @return a formatted String, never `null`
         */
        fun exactly(`in`: CharSequence): String {
            return eval(`in`).exactly
        }

        /**
         * Formats the given time delta, preserving all data.
         *
         *
         * Equivalent to `new HumanTime(in).getExactly()`
         *
         * @param l the time delta
         * @return a formatted String, never `null`
         */
        fun exactly(l: Long): String {
            return HumanTime(l).exactly
        }

        /**
         * Parses and formats the given char sequence, potentially removing some data to make the output easier to
         * understand.
         *
         *
         * Equivalent to `eval(in).getApproximately()`
         *
         * @param in the char sequence, may not be `null`
         * @return a formatted String, never `null`
         */
        fun approximately(`in`: CharSequence): String {
            return eval(`in`).approximately
        }

        /**
         * Formats the given time delta, preserving all data.
         *
         *
         * Equivalent to `new HumanTime(l).getApproximately()`
         *
         * @param l the time delta
         * @return a formatted String, never `null`
         */
        fun approximately(l: Long): String {
            return HumanTime(l).approximately
        }
    }
}
