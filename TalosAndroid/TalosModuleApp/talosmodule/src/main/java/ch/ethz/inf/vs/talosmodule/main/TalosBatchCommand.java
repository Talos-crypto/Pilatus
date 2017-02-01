package ch.ethz.inf.vs.talosmodule.main;

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

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;

/**
 * This class represents a batch command instruction in the cloud, i.e. multiple inserts
 * with one request.
 */
public class TalosBatchCommand {

    private String cmd;

    private List<TalosCipher[]> listOfArguments;

    /**
     * Creates a TalosCommand object
     * @param commandName the name of the corresponding stored procedure in the cloud
     */
    public TalosBatchCommand(String commandName) {
        this.cmd = commandName;
        this.listOfArguments = new ArrayList<>();
    }

    public void addCommand(TalosCipher[] args) {
        listOfArguments.add(args);
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

    public String[] getArgs(int index) {
        if(index<0 || index>=listOfArguments.size())
            throw new IllegalArgumentException("Invalid index " + index);
        return generateArgsFromCiphers(listOfArguments.get(index));
    }



    public TalosCipher getCommandAtIndex(int indexComamnd, int indexCipher) {
        if(indexComamnd<0 || indexComamnd>=listOfArguments.size())
            throw new IllegalArgumentException("Invalid index " + indexComamnd);
        TalosCipher[] temp = listOfArguments.get(indexComamnd);
        if(indexCipher<0 || indexCipher>=temp.length)
            throw new IllegalArgumentException("Invalid index " + temp.length);
        return temp[indexCipher];
    }

    public TalosCipher[] getArgsAtIndex(int indexComamnd) {
        if(indexComamnd<0 || indexComamnd>=listOfArguments.size())
            throw new IllegalArgumentException("Invalid index " + indexComamnd);
        return listOfArguments.get(indexComamnd);
    }

    /**
     *
     * @return number of commands
     */
    public int numCiphers() {
        return listOfArguments.size();
    }
}