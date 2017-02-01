package ch.ethz.inf.vs.talosmodule.prerelictests;

import android.test.InstrumentationTestCase;

import java.util.List;

import ch.ethz.inf.vs.talosmodule.main.SharedUser;
import ch.ethz.inf.vs.talosmodule.main.TalosResult;
import ch.ethz.inf.vs.talosmodule.main.User;
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

public class PreTalosTest extends InstrumentationTestCase {

    @Override
    public void setUp() throws Exception {

    }

    @Override
    public void tearDown() throws Exception {

    }

    public void testPRECloud() throws Exception {
        User u1 = new User("1", "1");
        User u2 = new User("2", "2");
        PreTalosModule module1 =new PreTalosModule(this.getInstrumentation().getContext(), "192.168.1.63", 8080, u1);
        PreTalosModule module2 =new PreTalosModule(this.getInstrumentation().getContext(), "192.168.1.63", 8080, u2);
        module1.registerUser(u1);
        module2.registerUser(u2);
        List<SharedUser> users = module1.getUsers(u1);
        module1.insertDataset(u1, 1);
        module1.insertDataset(u1, 2);
        module1.addSharedUser(u1, users.get(1));
        int a = module1.getSUM(u1,null).getInt();
        int b = module2.getSUM(u2, users.get(0)).getInt();
        assertEquals(a, b);
        module1.insertDataset(u1, 3);
        module1.insertDataset(u1, 4);
        Thread.sleep(1000);
        a = module1.getSUM(u1,null).getInt();
        b = module2.getSUM(u2, users.get(0)).getInt();
        assertEquals(a, b);

        users = module2.getAccesses(u2);
        assertEquals(1, users.size());
        users = module1.getMyShares(u1);
        assertEquals(1, users.size());
    }
}
