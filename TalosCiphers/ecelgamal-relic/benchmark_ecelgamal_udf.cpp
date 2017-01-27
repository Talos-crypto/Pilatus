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
#include "ecelgamal-relic.h"
}

using namespace std::chrono;

/*
 * Prints and error and closes the mysql connection.
 */
                void finish_with_error(MYSQL *con)
{
    fprintf(stderr, "%s\n", mysql_error(con));
    mysql_close(con);

}

int USE_HARDCODED_CRT_SIZE=0;

int encode_crt_ciph(char **out, int *size_out, gamal_ciphertext_t *ciphs, int num_partitions) {
    int iter=0;
    int lengths[num_partitions];
    int tot_len = 1;
    char *store;

    for(iter=0; iter<num_partitions; iter++) {
        lengths[iter] = get_encoded_ciphertext_size(ciphs[iter]);
        tot_len += lengths[iter];
    }

    *out = (char *) malloc(1 + tot_len);
    (*out)[0] = (char) num_partitions;
    store = *out;
    store += 1;

    for(iter=0; iter<num_partitions; iter++) {
        encode_ciphertext(store, lengths[iter], ciphs[iter]);
        store += lengths[iter];
    }
    *size_out = tot_len;
    return 0;
}

int decode_crt_ciph(gamal_ciphertext_t **ciphs, int *num_partitions, char *in, int size_in) {
    int iter=0;
    char *store;
    int temp_partitions;

    temp_partitions = (int) in[0];
    store = in;
    store += 1;

    *ciphs = (gamal_ciphertext_t *) malloc(temp_partitions * sizeof(gamal_ciphertext_t));

    for(iter=0; iter<temp_partitions; iter++) {
        decode_ciphertext((*ciphs)[iter], store, (int) size_in);
        store += get_encoded_ciphertext_size((*ciphs)[iter]);
    }
    *num_partitions = temp_partitions;
    return 0;
}


size_t create_cipher(char** temp, uint64_t msg, gamal_key_t key, int num_crt) {
    int res_size = 0;
    char * res;
    gamal_ciphertext_t cipher[num_crt];
    int iter = 0;

    for(iter=0; iter<num_crt; iter++) {
        gamal_encrypt(cipher[iter], key, msg+iter);
    }

    encode_crt_ciph(&res, &res_size, cipher, num_crt);
    *temp = res;

    for(iter=0; iter<num_crt; iter++) {
        gamal_cipher_clear(cipher[iter]);
    }

    return (size_t) res_size;
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
    //if (mysql_real_connect(*con, "localhost", "root", "",
    //"AddBenchDB", 0, NULL, 0) == NULL)
    //if (mysql_real_connect(*con, "localhost", "bimbo", "",
    //"AddBenchDB", 0, NULL, 0) == NULL)
    if (mysql_real_connect(*con, "eu-central-1.compute.amazonaws.com", "bench", "",
                           "AddBenchDB", 0, NULL, 0) == NULL)
    {
        finish_with_error(*con);
        return 1;
    }


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
    //char *query = "SELECT SQL_NO_CACHE ECG_SUM(Data) FROM Ciphers";
    char *query = "SELECT ECG_SUM_CRT(Data) FROM Ciphers";
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
int benchmark_udf_mysql(int sizes[], int sizes_len, int crt_size) {
    MYSQL *con;
    MYSQL_STMT *stmt;
    gamal_key_t key;
    gamal_ciphertext_t ciphertext;
    int test_size;
    int res_sum;
    dig_t res;

    gamal_generate_keys(key);

    std::cout << "META INFO Num Iterations: "<< experiment_iterations <<  std::endl;
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

        res_sum = 0;
        for(unsigned int i=0; i<test_size; i++) {
            char *encoded;
            size_t length;
            length = create_cipher(&encoded, i, key, crt_size);
            store_encoded(&con, &stmt, encoded, length);
            free(encoded);
            res_sum += i;
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
            //decode_ciphertext(ciphertext, sum, 1000);
            //gamal_decrypt(&res, key, ciphertext, 1);
            //std::cout << "should: " << res_sum << " is: " << res << std::endl;
        }


    }

    drop_table(&con);
    mysql_stmt_close(stmt);
    mysql_close(con);
}


int main(int argc, char* argv[]) {
    gamal_init();
    int steps[] = {50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 5000, 10000};
    int num_partitiions = 3;
    if(argc>=2) {
        num_partitiions= atoi(argv[1]);
    }
    benchmark_udf_mysql(steps, 17, num_partitiions);
    gamal_deinit();
    return 0;
}