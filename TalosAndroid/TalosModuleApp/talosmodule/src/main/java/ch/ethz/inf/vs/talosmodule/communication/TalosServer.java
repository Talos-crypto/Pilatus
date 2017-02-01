package ch.ethz.inf.vs.talosmodule.communication;

import java.util.List;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosBatchCommand;
import ch.ethz.inf.vs.talosmodule.main.TalosCipherResultData;
import ch.ethz.inf.vs.talosmodule.main.TalosCommand;
import ch.ethz.inf.vs.talosmodule.main.User;

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

public interface TalosServer {

    /**
     * Executes a Talos Command on the Talos Cloud
     * @param user the executing user (Signed in wth Google Sign-In)
     * @param cmd the Talos api command
     * @return a Talos Result Set
     * @throws TalosModuleException if something went wrong
     */
    public TalosCipherResultData execute(User user, TalosCommand cmd) throws TalosModuleException;

    /**
     * Executes a Talos batch command on the talos cloud. I.e multiple inserts in a batch
     * @param user the executing user (Signed-in wth Google Sign-In)
     * @param cmd the talos batch command
     * @return (nothing)
     * @throws TalosModuleException if something went wrong
     */
    public TalosCipherResultData execute(User user, TalosBatchCommand cmd) throws TalosModuleException;

    /**
     * Executes a talos command with the role of a shared user. Enables sharing data, if A shares
     * with B, B can execute Talos query commands in the name of A
     * @param user the executing user (Signed-in wth Google Sign-In) (B)
     * @param cmd the talos query command
     * @param shareUser the shared User (A)
     * @return
     * @throws TalosModuleException if something went wrong
     */
    public TalosCipherResultData execute(User user, TalosCommand cmd, SharedUser shareUser) throws TalosModuleException;

    /**
     * Register a Talos user in the talos cloud
     * @param user the user to register (Signed-in wth Google Sign-In)
     * @return true if success, else false
     * @throws TalosModuleException if something went wrong
     */
    public boolean register(User user) throws TalosModuleException;

    /**
     * Register a Talos user in the talos cloud, which wants to use the sharing property of the
     * Talos Cloud (Stores PK in the Cloud)
     * @param user the user to register (Signed-in wth Google Sign-In)
     * @return true if success, else false
     * @throws TalosModuleException if something went wrong
     */
    public boolean registerShared(User user) throws TalosModuleException;

    /**
     * Querries the users, which the current executing User can share his data with.
     * @param user the executing user (Signed-in wth Google Sign-In)
     * @return the sharable Users
     * @throws TalosModuleException if something went wrong
     */
    public List<SharedUser> getSharableUsers(User user) throws TalosModuleException;

    /**
     * Queries the users, which the current executing User is sharing with.
     * @param user the executing user (Signed-in wth Google Sign-In)
     * @return the shared with Users
     * @throws TalosModuleException if something went wrong
     */
    public List<SharedUser> getMySharedUsers(User user) throws TalosModuleException;

    /**
     * Queries the users, wich currently share their data with the executing User
     * @param user  the executing user (Signed-in wth Google Sign-In)
     * @return the Users, which allowed to access their data
     * @throws TalosModuleException
     */
    public List<SharedUser> getMyAccessableUsers(User user) throws TalosModuleException;

    /**
     * Shares the data of the executing User with the provided shared user.
     * @param user  the executing user (Signed-in wth Google Sign-In)
     * @param shareUser the User to share with
     * @return true if success, else false
     * @throws TalosModuleException if something went wrong
     */
    public boolean shareMyData(User user, SharedUser shareUser) throws TalosModuleException;


}
