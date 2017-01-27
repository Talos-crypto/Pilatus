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

#include <iostream>
#include <gmpxx.h>
#include <chrono>
#include <my_global.h>
#include <mysql.h>
#include <cstdlib>

extern "C" {
#include "pre-hom.h"
#include "test_pre_udf.h"
}

using namespace std::chrono;

//Implements a db PRE UDF benchmark

/*
 * Prints and error and closes the mysql connection.
 */
void finish_with_error(MYSQL *con)
{
    fprintf(stderr, "%s\n", mysql_error(con));
    mysql_close(con);

}

int USE_HARDCODED_CRT_SIZE=0;

int encode_ciphers(char **encoded, size_t *encoded_len, pre_ciphertext_t *cipher, int num_ciphers) {
    //hack
    int iter=0;
    char *cur_position;
    int cur_len = 0, temp;

    for(;iter<num_ciphers;iter++) {
        cur_len+=get_encoded_cipher_size(cipher[iter]);
    }

    if(USE_HARDCODED_CRT_SIZE<=0) {
        cur_len++;
    }

    *encoded = (char *) malloc((size_t) cur_len);

    if(USE_HARDCODED_CRT_SIZE<=0) {
        cur_position = (*encoded) + 1;
        (*encoded)[0] = (char) num_ciphers;
    } else {
        cur_position = *encoded;
    }

    for(iter=0 ;iter<num_ciphers;iter++) {
        temp = get_encoded_cipher_size(cipher[iter]);
        if(encode_cipher(cur_position, temp, cipher[iter])==STS_ERR) {
            return 1;
        }
        cur_position += temp;
    }
    *encoded_len = (size_t) cur_len;
    return 0;
}

size_t create_cipher_crt(char** temp, uint64_t msg, pre_keys_t key, int num_partitions) {
    size_t res_size = 0;
    pre_ciphertext_t cipher[num_partitions];
    char* res;
    for(int i=0; i<num_partitions; i++) {
        pre_encrypt(cipher[i], key, msg);
    }
    encode_ciphers(temp, &res_size, cipher, num_partitions);
    for(int i=0; i<num_partitions; i++) {
        pre_cipher_clear(cipher[i]);
    }
    return res_size;
}

size_t create_cipher_crt_level2(char** temp, uint64_t msg, pre_keys_t key, pre_re_token_t  token, int num_partitions) {
    size_t res_size = 0;
    pre_ciphertext_t cipher[num_partitions];
    char* res;
    for(int i=0; i<num_partitions; i++) {
        pre_ciphertext_t temp;
        pre_encrypt(temp, key, msg);
        pre_re_apply(token, cipher[i], temp);
        pre_cipher_clear(temp);
    }
    encode_ciphers(temp, &res_size, cipher, num_partitions);
    for(int i=0; i<num_partitions; i++) {
        pre_cipher_clear(cipher[i]);
    }
    return res_size;
}

/**
 * Connects to the mysql database with the given connection.
 */
int connect_to_db(MYSQL **con) {
    *con = mysql_init(NULL);

    if (*con == NULL)
    {
        fprintf(stderr, "mysql_init() failed\n");
        return 1;
    }
    //if (mysql_real_connect(*con, "localhost", "root", "talos?-73!",
      //                     "AddBenchDB", 0, NULL, 0) == NULL)
    //if (mysql_real_connect(*con, "localhost", "bimbo", "1234",
    //"AddBenchDB", 0, NULL, 0) == NULL)
    if (mysql_real_connect(*con, "ec2-52-59-255-221.eu-central-1.compute.amazonaws.com", "bench", "talos1234",
                         "AddBenchDB", 0, NULL, 0) == NULL)
    {
        finish_with_error(*con);
        return 1;
    }
    //char* url = "ec2-54-93-98-244.eu-central-1.compute.amazonaws.com";
    //if (mysql_real_connect(*con, url, "bench", "talos-02£23-!", "AddBenchDB", 0, NULL, 0) == NULL)


    return 0;
}

/*
 * Drops the table 'Ciphers' in the mysql db behind the given connection.
 */
int drop_table(MYSQL **con) {
    if (mysql_query(*con, "DROP TABLE IF EXISTS Ciphers")) {
        finish_with_error(*con);
        return 1;
    }

    return 0;
}

/*
 * Creates the table 'Ciphers' in the mysql db behind the given connection.
 * Drops the table if it already exists.
 */
int create_table(MYSQL **con) {
    if (mysql_query(*con, "DROP TABLE IF EXISTS Ciphers")) {
        finish_with_error(*con);
        return 1;
    }

    if (mysql_query(*con, "CREATE TABLE Ciphers(Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, Data BLOB)")) {
        finish_with_error(*con);
        return 1;
    }
    return 0;
}

/*
 * Cleans the table 'Ciphers' in the mysql db behind the given connection.
 */
int clean_table(MYSQL **con) {
    if (mysql_query(*con, "TRUNCATE TABLE Ciphers")) {
        finish_with_error(*con);
        return 1;
    }

    return 0;
}

/*
 * Creates the insert statement for ciphers into the table 'Ciphers' behind the given connection.
 */
int create_insert_statement(MYSQL **con, MYSQL_STMT **stmt) {
    char *st = "INSERT INTO Ciphers(Data) VALUES(?)";
    int len = strlen(st);

    *stmt = mysql_stmt_init(*con);
    if (mysql_stmt_prepare(*stmt, st, len)) {
        finish_with_error(*con);
        return 1;
    }
    return 0;
}

/*
 * Stores the encoded cipher of the given length in the db behind the given connection with the given statement.
 */
int store_encoded(MYSQL **con, MYSQL_STMT **stmt, char* encoded, size_t length) {
    MYSQL_BIND bind[1];
    memset(bind, 0, sizeof(bind));

    bind[0].buffer_type = MYSQL_TYPE_STRING;
    bind[0].buffer = encoded;
    bind[0].buffer_length = length;
    bind[0].is_null = 0;
    bind[0].length = 0;

    if (mysql_stmt_bind_param(*stmt, bind))
    {
        finish_with_error(*con);
        return 0;
    }

    if (mysql_stmt_execute(*stmt))
    {
        finish_with_error(*con);
        return 0;
    }

    return mysql_stmt_affected_rows(*stmt);
}

/*
 * Returns the cipher with Id=1 in the db behind the given connection.
 */
char* get_cipher(MYSQL **con) {
    if (mysql_query(*con, "SELECT Data FROM Ciphers WHERE Id=1"))
    {
        finish_with_error(*con);
    }

    MYSQL_RES *result = mysql_store_result(*con);

    if (result == NULL)
    {
        finish_with_error(*con);
    }

    MYSQL_ROW row = mysql_fetch_row(result);
    unsigned long *lengths = mysql_fetch_lengths(result);

    mysql_free_result(result);

    if (lengths == NULL) {
        finish_with_error(*con);
    }

    return row[0];
}

/*
 * Returns the result of the UDF for the stored ciphers up until the given max_id.
 */
char* get_sum(MYSQL **con, int max_id) {
    char *query = "SELECT PRE_REL_SUM(Data) FROM Ciphers";
    if (mysql_query(*con, query))
    {
        finish_with_error(*con);
    }

    MYSQL_RES *result = mysql_store_result(*con);

    if (result == NULL)
    {
        finish_with_error(*con);
    }

    MYSQL_ROW row = mysql_fetch_row(result);
    unsigned long *lengths = mysql_fetch_lengths(result);

    mysql_free_result(result);

    if (lengths == NULL) {
        finish_with_error(*con);
    }

    return row[0];
}


unsigned long int experiment_iterations=10;
int benchmark_udf_mysql(int num_partitions, int sizes[], int sizes_len, int use_level1) {
    MYSQL *con;
    MYSQL_STMT *stmt;
    pre_keys_t key, key_to;
    pre_re_token_t token;
    int test_size;

    pre_generate_keys(key);
    pre_generate_keys(key_to);
    pre_generate_re_token(token, key, key_to->pk_2);

    std::cout << "META INFO Num Iterations: "<< experiment_iterations << ", Num Partitions: " <<  num_partitions << ", Level " << (use_level1 == 1 ? 1 : 2) << std::endl;
    connect_to_db(&con);
    create_table(&con);
    create_insert_statement(&con, &stmt);

    std::cout << "Number of Ciphers, ";
    for (int iter = 0; iter < experiment_iterations; iter++) {
        if(iter!=experiment_iterations-1) {
            std::cout << "Round" << iter <<  ", ";
        } else {
            std::cout << "Round" << iter << std::endl;
        }
    }

    for (int iter = 0; iter < sizes_len; iter++) {
        test_size = sizes[iter];
        clean_table(&con);

        for(unsigned int i=0; i<test_size; i++) {
            char *encoded;
            size_t length;
            if(use_level1) {
                length = create_cipher_crt(&encoded, i, key, num_partitions);
            } else {
                length = create_cipher_crt_level2(&encoded, i, key, token, num_partitions);
            }
            store_encoded(&con, &stmt, encoded, length);
            free(encoded);
        }

        std::cout << (unsigned long) test_size << ", ";
        for (int iteration = 0; iteration < experiment_iterations+1; iteration++) {
            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            get_sum(&con, test_size);
            high_resolution_clock::time_point t2 = high_resolution_clock::now();
            auto ns = duration_cast<nanoseconds>(t2-t1).count();
            if(iteration>0)
                if(iteration != experiment_iterations)
                    std::cout << (unsigned long) ns << ", ";
                else
                    std::cout << (unsigned long) ns << std::endl;
        }


    }

    drop_table(&con);
    mysql_stmt_close(stmt);
    mysql_close(con);
}


int main(int argc, char* argv[]) {
    int steps[] = {50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 5000, 10000};
    int num_partitiions = 3;
    int use_level1 = 0;
    pre_init();
    if(argc>=3) {
        num_partitiions= atoi(argv[1]);
        use_level1= atoi(argv[2]);
    }
    benchmark_udf_mysql(num_partitiions, steps, 17, use_level1);
    pre_deinit();
    return 0;
}