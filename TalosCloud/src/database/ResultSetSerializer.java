package database;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.sql.*;

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
 * Serializes a Database ResultSet to JSON using the jackson library.
 * Template fromhttp://stackoverflow.com/questions/6514876/most-effecient-conversion-of-resultset-to-json
 */
public class ResultSetSerializer extends JsonSerializer<ResultSet> {

    public static class ResultSetSerializerException extends JsonProcessingException {

        protected ResultSetSerializerException(String msg) {
            super(msg);
        }
    }

    @Override
    public Class<ResultSet> handledType() {
        return ResultSet.class;
    }

    @Override
    public void serialize(ResultSet rs, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
            String[] columnNames = new String[numColumns];
            int[] columnTypes = new int[numColumns];

            for (int i = 0; i < columnNames.length; i++) {
                columnNames[i] = rsmd.getColumnLabel(i + 1);
                columnTypes[i] = rsmd.getColumnType(i + 1);
            }

            jgen.writeStartArray();

            while (rs.next()) {

                boolean b;
                long l;
                double d;

                jgen.writeStartObject();

                for (int i = 0; i < columnNames.length; i++) {

                    jgen.writeFieldName(columnNames[i]);
                    switch (columnTypes[i]) {

                        case Types.INTEGER:
                            l = rs.getInt(i + 1);
                            if (rs.wasNull()) {
                                jgen.writeNull();
                            } else {
                                jgen.writeNumber(l);
                            }
                            break;

                        case Types.BIGINT:
                        case Types.DECIMAL:
                        case Types.NUMERIC:
                            jgen.writeString(rs.getString(i + 1));
                            break;

                        case Types.FLOAT:
                        case Types.REAL:
                        case Types.DOUBLE:
                            d = rs.getDouble(i + 1);
                            if (rs.wasNull()) {
                                jgen.writeNull();
                            } else {
                                jgen.writeNumber(d);
                            }
                            break;

                        case Types.NVARCHAR:
                        case Types.VARCHAR:
                        case Types.LONGNVARCHAR:
                        case Types.LONGVARCHAR:
                            jgen.writeString(rs.getString(i + 1));
                            break;

                        case Types.BOOLEAN:
                        case Types.BIT:
                            b = rs.getBoolean(i + 1);
                            if (rs.wasNull()) {
                                jgen.writeNull();
                            } else {
                                jgen.writeBoolean(b);
                            }
                            break;

                        case Types.BINARY:
                        case Types.VARBINARY:
                        case Types.LONGVARBINARY:
                            //ec-elgamal res
                            byte[] res = rs.getBytes(i + 1);
                            final StringBuilder builder = new StringBuilder();
                            builder.append("0x");
                            for (byte bt : res) {
                                builder.append(String.format("%02x", bt));
                            }
                            jgen.writeString(builder.toString());
                            //jgen.writeString(rs.getString(i + 1));
                            break;

                        case Types.TINYINT:
                        case Types.SMALLINT:
                            l = rs.getShort(i + 1);
                            if (rs.wasNull()) {
                                jgen.writeNull();
                            } else {
                                jgen.writeNumber(l);
                            }
                            break;

                        case Types.DATE:
                            //provider.defaultSerializeDateValue(rs.getDate(i + 1), jgen);
                            jgen.writeString(rs.getDate(i+1).toString());
                            break;
                        case Types.TIME:
                            jgen.writeString(rs.getTime(i+1).toString());
                            break;
                        case Types.TIMESTAMP:
                            provider.defaultSerializeDateValue(rs.getTime(i + 1), jgen);
                            break;

                        case Types.BLOB:
                            Blob blob = rs.getBlob(i);
                            provider.defaultSerializeValue(blob.getBinaryStream(), jgen);
                            blob.free();
                            break;

                        case Types.CLOB:
                            Clob clob = rs.getClob(i);
                            provider.defaultSerializeValue(clob.getCharacterStream(), jgen);
                            clob.free();
                            break;

                        case Types.ARRAY:
                            throw new RuntimeException("Not Allowed");

                        case Types.STRUCT:
                            throw new RuntimeException("Not Allowed");

                        case Types.DISTINCT:
                            throw new RuntimeException("Not Allowed");

                        case Types.REF:
                            throw new RuntimeException("Not Allowed");

                        case Types.JAVA_OBJECT:
                        default:
                            provider.defaultSerializeValue(rs.getObject(i + 1), jgen);
                            break;
                    }
                }

                jgen.writeEndObject();
            }

            jgen.writeEndArray();

        } catch (SQLException e) {
            throw new ResultSetSerializerException(e.getMessage());
        }
    }
}
