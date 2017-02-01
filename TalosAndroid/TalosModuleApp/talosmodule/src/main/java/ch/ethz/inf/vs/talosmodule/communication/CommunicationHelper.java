package ch.ethz.inf.vs.talosmodule.communication;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ch.ethz.inf.vs.talosmodule.exceptions.CommTalosModuleException;

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
 * Created by lukas on 09.11.15.
 */
public abstract class CommunicationHelper {

    private String addr;

    public CommunicationHelper(String protocol, String ip, int port) {
        this.addr = protocol+"://"+ ip + ":" + port;
    }

    public String sendMessage(String content, String resource) throws CommTalosModuleException {
        String response = null;
        HttpURLConnection urlConnection = null;
        OutputStream out = null;
        BufferedInputStream in = null;
        try {
            urlConnection = getConnection(new URL(addr+resource));
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(content.getBytes());
            out.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new CommTalosModuleException("Error Response Code: " +responseCode);
            }

            in = new BufferedInputStream(urlConnection.getInputStream());
            response = getStringFromInput(in, "UTF-8");
            in.close();

        } catch (IOException e) {
            throw new CommTalosModuleException(e.getMessage());
        } catch (Exception e) {
            throw new CommTalosModuleException(e.getMessage());
        } finally {
            try {
                if(urlConnection!=null)
                    urlConnection.disconnect();
                if(out!=null   )
                    out.close();
                if(in!=null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return response;
    }

    private static String getStringFromInput(InputStream in, String format) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, format));
        String cur = bufferedReader.readLine();
        while (cur != null) {
            sb.append(cur);
            cur = bufferedReader.readLine();
        }
        return sb.toString();
    }

    public abstract HttpURLConnection getConnection(URL url) throws IOException;


}
