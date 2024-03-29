/**
 * Copyright 2017-2020 European Union, interactive instruments GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.bsxm.topox;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class DataCompression {

    private DataCompression() {}

    /**
     * Get the last 32 bits
     *
     * @param compressedValue
     *            compressed long value
     * @return last 32 bits
     */
    public static int getLeft(final long compressedValue) {
        return (int) (compressedValue >> 32);
    }

    /**
     * Get the first 32 bits
     *
     * @param compressedValue
     *            compressed long value
     * @return first 32 bits
     */
    public static int getRight(final long compressedValue) {
        return (int) compressedValue;
    }

    /**
     * Compress two ints to one long
     *
     * @param left
     *            int to shift the last 32 bits
     * @param right
     *            int to shift the first 32 bits
     * @return a long with both ints
     */
    public static long compress(final int left, final int right) {
        return toLeft(left) | toRight(right);
    }

    /**
     * 32 bit shift to the left
     *
     * @param left
     *            int to shift
     * @return a long with 32 bit left shifted
     */
    public static long toLeft(final int left) {
        return (((long) left) << 32);
    }

    /**
     * 32 bit shift to the right
     *
     * @param right
     *            int to shift
     * @return a long with 32 bit right shifted
     */
    public static long toRight(final int right) {
        return (right & 0xFFFFFFFFL);
    }

    public static int makeCompressedNodeIndex(final byte dbIndex, final int objectGeoDiffIndex) {
        return (((dbIndex) << 24) | objectGeoDiffIndex & 0xFFFFFF);
    }

    /**
     * Extract the db index from a compressed long index
     *
     * @param compressedIndex
     *            compressed long index
     * @return db index as int
     */
    public static int dbIndex(final long compressedIndex) {
        return (int) (compressedIndex >>> 56);
    }

    /**
     * Extract the object index from a compressed long index
     *
     * @param compressedIndex
     *            compressed long index
     * @return object index as int
     */
    public static int objectIndex(final long compressedIndex) {
        return ((int) (compressedIndex >>> 32) & 0xFFFFFF);
    }

    /**
     * Extract the object index from a compressed long index
     *
     * @param compressedIndex
     *            compressed long index
     * @return BaseX integer pre value of the object
     */
    public static int preObject(final long compressedIndex) {
        return getRight(compressedIndex) -
                objectIndex(compressedIndex);
    }
}
