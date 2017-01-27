//
// Created by lubu on 05.12.16.
//

#include <iostream>
#include <gmpxx.h>
#include <chrono>
#include <my_global.h>
#include <mysql.h>
#include <cstdlib>

extern "C" {
#include "key_hom.h"
#include <stdlib.h>
#include <time.h>
}

using namespace std::chrono;

/*
 * Prints and error and closes the mysql connection.
 */void finish_with_error(MYSQL *con) {
    fprintf(stderr, "%s\n", mysql_error(con));
    mysql_close(con);

}

int USE_HARDCODED_CRT_SIZE=0;

size_t create_cipher(unsigned char** temp, unsigned char* msg, size_t msg_size, KHC_Key *key) {
    size_t res_size = 0;
    KHC_Cipher *cipher;
    unsigned char* out;
    KHC_encrypt(msg, msg_size, key, 0, &cipher);
    KHC_Cipher_encode(key->group, cipher, &out, &res_size);
    KHC_Cipher_clear(cipher);
    *temp = out;
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
        if (mysql_real_connect(*con, "localhost", "bimbo", "1234",
        "AddBenchDB", 0, NULL, 0) == NULL)
        //if (mysql_real_connect(*con, "ec2-52-59-255-221.eu-central-1.compute.amazonaws.com", "bench", "talos1234",
        //                     "AddBenchDB", 0, NULL, 0) == NULL)
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
 * Creates the insert statement for ciphers into the table 'Ciphers' behind the given connection.
 */
int create_select_statement(MYSQL **con, MYSQL_STMT **stmt) {
    char *query = "SELECT KEY_HOM_UPDATE(Data, ?) FROM Ciphers";
    int len = (int) strlen(query);

    *stmt = mysql_stmt_init(*con);
    if (mysql_stmt_prepare(*stmt, query, (unsigned long) len)) {
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
int get_sum(MYSQL **con, MYSQL_STMT **stmt, char *key, unsigned long key_len) {
    MYSQL_BIND bind[1];
    memset(bind, 0, sizeof(bind));

    bind[0].buffer_type = MYSQL_TYPE_STRING;
    bind[0].buffer = key;
    bind[0].buffer_length = key_len;
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

    /*MYSQL_RES *result = mysql_stmt_store_result(*stmt);

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

    return row[0]*/
    return mysql_stmt_affected_rows(*stmt);
}

unsigned char *gen_rdm_bytestream (size_t num_bytes)
{
    unsigned char *stream =(unsigned char *) malloc (num_bytes);
    size_t i;

    for (i = 0; i < num_bytes; i++)
    {
        stream[i] = rand ();
    }

    return stream;
}


unsigned long int experiment_iterations=10;
int benchmark_udf_mysql(int sizes[], int sizes_len, int data_size, EC_GROUP *group) {
    MYSQL *con;
    MYSQL_STMT *stmt, *query_stmt;
    KHC_Key *key1, *key2, *key1_to_key2;
    int test_size;
    unsigned char *encoded_token;
    size_t encoded_token_size;

    key1 = KHC_new_key_params(group, 16);
    key2 = KHC_new_key_params(group, 16);
    KHC_create_re_enc_token(key1, key2, &key1_to_key2);
    KHC_Key_encode(key1_to_key2, &encoded_token, &encoded_token_size);

    std::cout << "META INFO Num Iterations: "<< experiment_iterations << ", Data Size: " <<  data_size << std::endl;
    connect_to_db(&con);
    create_table(&con);

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
        create_insert_statement(&con, &stmt);
        create_select_statement(&con, &query_stmt);

        for(unsigned int i=0; i<test_size; i++) {
            unsigned char *encoded;
            unsigned char *tmp;
            size_t length;
            tmp = gen_rdm_bytestream(data_size);
            length = create_cipher(&encoded, tmp, data_size, key1);
            store_encoded(&con, &stmt, (char*) encoded, length);
            free(encoded);
            free(tmp);
        }

        std::cout << (unsigned long) test_size << ", ";
        for (int iteration = 0; iteration < experiment_iterations+1; iteration++) {
            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            get_sum(&con, &query_stmt, (char *) encoded_token, encoded_token_size);
            high_resolution_clock::time_point t2 = high_resolution_clock::now();
            auto ns = duration_cast<nanoseconds>(t2-t1).count();
            if(iteration>0)
            if(iteration != experiment_iterations)
                std::cout << (unsigned long) ns << ", ";
            else
                std::cout << (unsigned long) ns << std::endl;
        }
        mysql_stmt_close(stmt);
        mysql_stmt_close(query_stmt);
    }

    drop_table(&con);
    mysql_close(con);
}


int main(int argc, char* argv[]) {
    int steps[] = {50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 5000, 10000};
    int data_size = 10;
    if(argc>=2) {
        data_size= atoi(argv[1]);
    }
    benchmark_udf_mysql(steps, 17, data_size, EC_GROUP_new_by_curve_name(NID_X9_62_prime256v1));
    return 0;
}