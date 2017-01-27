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
#include <time.h>
#include <stdlib.h>
}
using namespace std::chrono;

//Implements a DB benchmark for the Plain sum for comparison

/*
 * Prints and error and closes the mysql connection.
 */
void finish_with_error(MYSQL *con)
{
    fprintf(stderr, "%s\n", mysql_error(con));
    mysql_close(con);

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
    //"AddBenchDB", 0, NULL, 0) == NULL)
    //if (mysql_real_connect(*con, "localhost", "bimbo", "1234",
    //"AddBenchDB", 0, NULL, 0) == NULL)
    if (mysql_real_connect(*con, "ec2-54-93-54-190.eu-central-1.compute.amazonaws.com", "bench", "talos1234",
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

    if (mysql_query(*con, "CREATE TABLE Ciphers(Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, Data INTEGER)")) {
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
int store_encoded(MYSQL **con, MYSQL_STMT **stmt, int num) {
    MYSQL_BIND bind[1];
    memset(bind, 0, sizeof(bind));

    bind[0].buffer_type = MYSQL_TYPE_LONG;
    bind[0].buffer = (void *) &num;
    bind[0].buffer_length = 0;
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
    char *query = "SELECT SQL_NO_CACHE SUM(Data) FROM Ciphers";
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
int benchmark_udf_mysql(int sizes[], int sizes_len) {
    MYSQL *con;
    MYSQL_STMT *stmt;
    int test_size;

    std::cout << "META INFO Num Iterations: "<< experiment_iterations << std::endl;
    connect_to_db(&con);
    create_table(&con);
    create_insert_statement(&con, &stmt);
    srand(time(NULL));
    std::cout << "Number of Integers, ";
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
            int r = rand();
            store_encoded(&con, &stmt, r);
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
    benchmark_udf_mysql(steps, 17);
    return 0;
}