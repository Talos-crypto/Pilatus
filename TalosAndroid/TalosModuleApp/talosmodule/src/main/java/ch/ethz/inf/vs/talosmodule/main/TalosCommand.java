package ch.ethz.inf.vs.talosmodule.main;

import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;

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
 * A command represents a call to a stored procedure in the cloud.
 * Therefore, a TalosCommand requires the name of the procedure and an array of TalosCiphers as an input.
 * Only TalosCiphers are allowed, as the cloud should not have any plain data.
 * If the developer wants to store a dataitem in plaintext on the cloud, a PlainCipher can be created.
 * A TalosCommand can be executed  on the cloud by using the the execute function of the TalosServer object.
 */
public class TalosCommand {

    private String cmd;

    private TalosCipher[] args;

    /**
     * Creates a TalosCommand object
     * @param commandName the name of the corresponding stored procedure in the cloud
     * @param args the arguments (TalosCiphers) of the stored procedure
     */
    public TalosCommand(String commandName, TalosCipher[] args) {
        this.cmd = commandName;
        this.args = args;
    }

    private String[] generateArgsFromCiphers(TalosCipher[] ciphArgs) {
        String[] res = new String[ciphArgs.length];
        for(int i=0; i<ciphArgs.length; i++) {
            res[i] = ciphArgs[i].getStringRep();
        }
        return res;
    }

    /**
     *
     * @return the name of the command
     */
    public String getCommandName() {
        return cmd;
    }

    public String[] getArgs() {
        return generateArgsFromCiphers(args);
    }

    /**
     *
     * @param index the index of the argument
     * @return the TalosCipher at the index (aliasing)
     */
    public TalosCipher getCipherAtIndex(int index) {
        if(index<0 || index>=args.length)
            throw new IllegalArgumentException("Invalid index " + index);
        return args[index];
    }

    /**
     *
     * @return number of arguments of the command
     */
    public int numCiphers() {
        return args.length;
    }
}
