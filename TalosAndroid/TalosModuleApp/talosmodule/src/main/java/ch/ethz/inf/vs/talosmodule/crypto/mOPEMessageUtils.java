package ch.ethz.inf.vs.talosmodule.crypto;

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

public class mOPEMessageUtils {
	
	public static final String MESSAGE_TOKEN_DELIM = "#";
	public static final String MESSAGE_DELIM = "\n";

	public enum MessageType {
		DECIDE_START ("DECIDE_START"),
		DECIDE_ORDER ("DECIDE_ORDER"),
		DECIDE_REPLY ("DECIDE_REPLY"),
		DECIDE_FOUND ("DECIDE_FOUND"),
		INSERT_SUCC ("INSERT_SUCC"),
		QUERY_RESULT ("QUERY_RESULT"),
		ERROR_CODE ("ERROR_CODE");
		
		private final String name;
		
		private MessageType(String s) {
			this.name = s;
		}
		
		public boolean compareTO(String other) {
			return name.equals(other);
		}
		
		public String toString() {
			return this.name;
		}
		
		 public static MessageType fromString(String text) {
		    if (text != null) {
		      for (MessageType b : MessageType.values()) {
		        if (text.equalsIgnoreCase(b.name)) {
		          return b;
		        }
		      }
		    }
		    return null;
		 }
	}
	
	
}
