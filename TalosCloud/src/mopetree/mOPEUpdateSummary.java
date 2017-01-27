package mopetree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * A summary of the updated nodes.
 */
public class mOPEUpdateSummary<T> implements Iterable<Tuple<BigInteger, T>>{

    private List<Tuple<BigInteger, T>> updates = new ArrayList<>();

    private BigInteger min = BigInteger.ZERO;

    private BigInteger max = BigInteger.ZERO;

    public mOPEUpdateSummary() {
    }

    public void setMin(BigInteger min) {
        this.min = min;
    }

    public void setMax(BigInteger max) {
        this.max = max;
    }

    public void pushUpdate(T value, BigInteger newEncoding) {
        updates.add(new Tuple<>(newEncoding, value));
    }

    public List<T> getChangedKeys() {
        ArrayList<T> res = new ArrayList<>();
        for(Tuple<BigInteger, T> t : updates)
            res.add(t.b);
        return res;
    }

    public BigInteger getEncodingForValue(T value) throws RuntimeException {
        for(Tuple<BigInteger, T> tuple : updates) {
            if(value.equals(tuple.b)) {
                return tuple.a;
            }
        }
        throw new RuntimeException("Value not found: " + value);
    }

    public BigInteger getEncodingForValueAndRemove(T value) throws RuntimeException {
        Iterator<Tuple<BigInteger, T>> iter = updates.iterator();
        while (iter.hasNext()) {
            Tuple<BigInteger, T> tuple = iter.next();
            if(value.equals(tuple.b)) {
                iter.remove();
                return tuple.a;
            }
        }
        throw new RuntimeException("Value not found: " + value);
    }

    public boolean containsValue(T value) {
        for(Tuple<BigInteger, T> tuple : updates) {
            if(value.equals(tuple.b)) {
                return true;
            }
        }
        return false;
    }

    //O(n^2)
    public void mergeFollowedBy(mOPEUpdateSummary<T> other) {
        for(Tuple<BigInteger, T> replace : other.updates) {
            boolean found = false;
            for(Tuple<BigInteger, T> cur : this.updates) {
                if(cur.b.equals(replace.b)) {
                    found = true;
                    cur.b = replace.b;
                    break;
                }
            }
            if(!found) {
                this.updates.add(replace);
            }
        }
    }

    public boolean hasUpdates() {
        return !updates.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Tuple<BigInteger, T> t : updates) {
            sb.append(t.b).append("->").append(t.a).append("::");
        }
        if(sb.length()>2)
            sb.setLength(sb.length()-2);
        return sb.toString();
    }

    @Override
    public Iterator<Tuple<BigInteger, T>> iterator() {
        return updates.iterator();
    }
}
