package ch.ethz.inf.vs.talosmodule.main;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;

/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Represent the final decrypted result of a cloud request.
 * Design is oriented on the JDBC ResultSet.
 *
 */
public interface TalosResult {

    /**
     * Moves the current cursor to the next row.
     * @return true if there is a next row, false if there is no next row
     * @throws TalosModuleException
     */
    public boolean next() throws TalosModuleException;

    /**
     * Moves the current cursor to the previous row
     * @return true if there is a previous row, false if there is no previous row
     * @throws TalosModuleException
     */
    public boolean previous() throws TalosModuleException;

    /**
     * Access the number columns.
     * @return the number of columns in the result.
     * @throws TalosModuleException
     */
    public int getNumberOfColumns() throws TalosModuleException;

    /**
     * Find the column name of a given index.
     * @param index the index of a column
     * @return the name of the column.
     * @throws TalosModuleException
     */
    public String getColumnName(int index) throws TalosModuleException;

    /**
     * Moves the cursor to the last row.
     * @return true if there is a last row, else false
     * @throws TalosModuleException
     */
    public boolean last() throws TalosModuleException;

    /**
     * Moves the cursor to the first row.
     * @return true if there is a first row, else false
     * @throws TalosModuleException
     */
    public boolean first() throws TalosModuleException;

    /**
     * Get the integer representation of the value in
     * the current row at the given index
     * @param index the index of the column,
     * @return the integer representation of the value.
     * @throws TalosModuleException
     */
    public int getInt(int index) throws TalosModuleException;

    /**
     * Get the integer representation of the value in
     * the current row at the given index
     * @param colname the name of the column,
     * @return the integer representation of the value.
     * @throws TalosModuleException
     */
    public int getInt(String colname) throws TalosModuleException;

    /**
     * Get the long representation of the value in
     * the current row at the given index
     * @param index the index of the column,
     * @return the long representation of the value.
     * @throws TalosModuleException
     */
    public long getLong(int index) throws TalosModuleException;

    /**
     * Get the long representation of the value in
     * the current row at the given index
     * @param colname the name of the column,
     * @return the long representation of the value.
     * @throws TalosModuleException
     */
    public long getLong(String colname) throws TalosModuleException;

    /**
     * Get the String representation of the value in
     * the current row at the given index.
     * @param index the index of the column,
     * @return the String representation of the value.
     * @throws TalosModuleException
     */
    public String getString(int index) throws TalosModuleException;

    /**
     * Get the String representation of the value in
     * the current row at the given index.
     * @param colname the name of the column,
     * @return the String representation of the value.
     * @throws TalosModuleException
     */
    public String getString(String colname) throws TalosModuleException;

    /**
     * Get the double representation of the value in
     * the current row at the given index
     * @param colname the name of the column,
     * @param precision the precision used for decoding i.e value:123214 precision 2 -> 1232.14,
     * @return the double representation of the value.
     * @throws TalosModuleException if decode fails
     */
    public double getDouble(String colname,  int precision) throws TalosModuleException;

    /**
     * Get the double representation of the value in
     * the current row at the given index
     * @param index the index of the column
     * @param precision the precision used for decoding i.e value:123214 precision 2 -> 1232.14,
     * @return the double representation of the value.
     * @throws TalosModuleException if decode fails
     */
    public double getDouble(int index,  int precision) throws TalosModuleException;

    /**
     * Get the float representation of the value in
     * the current row at the given index
     * @param colname the name of the column,
     * @param precision the precision used for decoding i.e value:123214 precision 2 -> 1232.14,
     * @return the long representation of the value.
     * @throws TalosModuleException if decode fails
     */
    public float getFloat(String colname, int precision) throws TalosModuleException;

    /**
     * Get the float representation of the value in
     * the current row at the given index
     * @param index the index of the column
     * @param precision the precision used for decoding i.e value:123214 precision 2 -> 1232.14,
     * @return the long representation of the value.
     * @throws TalosModuleException if decode fails
     */
    public float getFloat(int index, int precision) throws TalosModuleException;

}
