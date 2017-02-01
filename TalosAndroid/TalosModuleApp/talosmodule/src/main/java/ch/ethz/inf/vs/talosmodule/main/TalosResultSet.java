package ch.ethz.inf.vs.talosmodule.main;

import java.math.BigDecimal;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;

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
 * Represent the final decrypted result of a cloud request
 */
public final class TalosResultSet implements TalosResult {

    private final List<TalosResultSetRow> rows;

    private final ListIterator<TalosResultSetRow> iterator;

    private String[] colNames;

    private TalosResultSetRow curRow = null;

    public TalosResultSet(final List<TalosResultSetRow> rows) {
        this.rows = rows;
        iterator = rows.listIterator();
        if(rows.isEmpty()) {
            colNames = new String[0];
        } else {
            initArray(rows.get(0).keySet());
        }

    }

    private void initArray(Set<String> cols) {
        int index = 0;
        colNames = new String[cols.size()];
        for(String colname : cols) {
            colNames[index++] = colname;
        }
    }

    private boolean hasRows() {
        return !rows.isEmpty();
    }

    private void checkIndex(int index) throws TalosModuleException{
        if(!(index>=0 && index<colNames.length))
            throw new TalosModuleException("Invalid index: " +index);
    }

    private void checkCurRow() throws TalosModuleException {
        if(curRow==null)
            throw new TalosModuleException("No row selected");
    }

    private TalosValue getValueFromIndex(int index) throws TalosModuleException {
        checkIndex(index);
        checkCurRow();
        TalosValue val = curRow.get(colNames[index]);
        return val;
    }

    private TalosValue getValueFromName(String colname) throws TalosModuleException {
        checkCurRow();
        TalosValue val = curRow.get(colname);
        if(val==null)
            throw new TalosModuleException("Column name not found: "+colname);
        return val;
    }


    @Override
    public boolean next() throws TalosModuleException {
        try {
            curRow = iterator.next();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }

    }

    @Override
    public boolean previous() throws TalosModuleException {
        try {
            curRow = iterator.previous();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public int getNumberOfColumns() throws TalosModuleException {
        return colNames.length;
    }

    @Override
    public String getColumnName(int index) throws TalosModuleException {
        checkIndex(index);
        return colNames[index];
    }

    @Override
    public boolean last() throws TalosModuleException {
        while (iterator.hasNext()) {
            curRow = iterator.next();
        }
        return true;
    }

    @Override
    public boolean first() throws TalosModuleException {
        if(curRow == null) {
            this.next();
        } else  {
            while (iterator.hasPrevious()) {
                curRow = iterator.previous();
            }
        }
        return true;
    }

    @Override
    public int getInt(int index) throws TalosModuleException {
        TalosValue val = getValueFromIndex(index);
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                return val.getInt();
            case STR:
            case UNKNOWN:
                try {
                    return Integer.valueOf(val.getString());
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public int getInt(String colname) throws TalosModuleException {
        TalosValue val = getValueFromName(colname);
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                return val.getInt();
            case STR:
            case UNKNOWN:
                try {
                    return Integer.valueOf(val.getString());
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public long getLong(int index) throws TalosModuleException {
        TalosValue val = getValueFromIndex(index);
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                return val.getLong();
            case STR:
            case UNKNOWN:
                try {
                    return Long.valueOf(val.getString());
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public long getLong(String colname) throws TalosModuleException {
        TalosValue val = getValueFromName(colname);
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                return val.getLong();
            case STR:
            case UNKNOWN:
                try {
                    return Long.valueOf(val.getString());
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public String getString(int index) throws TalosModuleException {
        TalosValue val = getValueFromIndex(index);
        switch (val.getType()) {
            case INT_32:
                return String.valueOf(val.getInt());
            case INT_64:
                return String.valueOf(val.getLong());
            case STR:
            case UNKNOWN:
                return val.getString();
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public String getString(String colname) throws TalosModuleException {
        TalosValue val = getValueFromName(colname);
        switch (val.getType()) {
            case INT_32:
                return String.valueOf(val.getInt());
            case INT_64:
                return String.valueOf(val.getLong());
            case STR:
            case UNKNOWN:
                return val.getString();
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public double getDouble(String colname, int precision) throws TalosModuleException {
        TalosValue val = getValueFromName(colname);
        long temp;
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                temp = val.getLong();
                return transform(temp, precision).doubleValue();
            case STR:
            case UNKNOWN:
                try {
                    temp = Long.valueOf(val.getString());
                    return transform(temp, precision).doubleValue();
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public double getDouble(int index, int precision) throws TalosModuleException {
        TalosValue val = getValueFromIndex(index);
        long temp;
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                temp = val.getLong();
                return transform(temp, precision).doubleValue();
            case STR:
            case UNKNOWN:
                try {
                    temp = Long.valueOf(val.getString());
                    return transform(temp, precision).doubleValue();
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public float getFloat(String colname, int precision) throws TalosModuleException {
        TalosValue val = getValueFromName(colname);
        long temp;
        switch (val.getType()) {
            case INT_32:
            case INT_64:
                temp = val.getLong();
                return transform(temp, precision).floatValue();
            case STR:
            case UNKNOWN:
                try {
                    temp = Long.valueOf(val.getString());
                    return transform(temp, precision).floatValue();
                } catch (NumberFormatException e) {
                    throw new TalosModuleException("Wrong Format");
                }
        }
        throw new TalosModuleException("Wrong Format");
    }

    @Override
    public float getFloat(int index, int precision) throws TalosModuleException {
        return 0;
    }

    private static BigDecimal transform(long value, int radix) {
        BigDecimal res = BigDecimal.valueOf(value);
        res = res.movePointLeft(radix);
        return res;
    }


}
