package ch.ethz.inf.vs.talosmodule.crypto.enclayers;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
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

public abstract class EncLayer {

    public abstract TalosCipher encrypt(TalosValue field) throws TalosModuleException;

    public abstract TalosValue decrypt(TalosCipher field) throws TalosModuleException;

    public abstract TalosCipher getCipherFromString(String in);

    public enum EncLayerType {
        PLAIN,
        HOM,
        DET,
        DET_JOIN,
        RND,
        OPE;

        public static EncLayerType getEncType(String type) {
            EncLayerType res = null;
            String upper = type.toUpperCase();
            switch (upper) {
                case "PLAIN":
                    res = PLAIN;
                    break;
                case "HOM":
                    res = HOM;
                    break;
                case "DET":
                    res = DET;
                    break;
                case "RND":
                    res = RND;
                    break;
                case "OPE":
                    res = OPE;
                    break;
                case "DET_JOIN":
                    res = DET_JOIN;
                    break;
                default:
                    break;
            }
            return res;
        }

        public boolean isMain() {
            return this.isDeterministic() || this.equals(RND) || this.equals(PLAIN);
        }

        public boolean isDeterministic() {
            return this.equals(DET_JOIN) || this.equals(DET) || this.equals(PLAIN);
        }

        public boolean hasMultipleLayers() {
            return this.isRND() || this.isDeterministic();
        }

        public boolean isOPE() {
            return this.equals(OPE) || this.equals(PLAIN);
        }

        public boolean isHOM() {
            return this.equals(HOM) || this.equals(PLAIN);
        }

        public boolean isJOIN() {
            return this.equals(DET_JOIN) || this.equals(PLAIN);
        }

        public boolean isRND() {
            return this.equals(RND);
        }

        public boolean isPLAIN() {
            return this.equals(PLAIN);
        }

    }

}
