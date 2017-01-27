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

extern "C" {
#include "ecelgamal-relic.h"
}

using namespace std::chrono;

unsigned long int experiment_iterations=10;
int benchmark(int num_partitions, int sizes[], int sizes_len) {
    gamal_ciphertext_t ciphers[sizes[sizes_len-1]];
    gamal_ciphertext_t sum;
    gamal_key_t key;
    int test_size, sum_plain=0;
    dig_t res;

    gamal_init_bsgs_table(1<<16);

    gamal_cipher_new(sum);

    for (int iter = 0; iter < sizes_len; iter++) {
        test_size = sizes[iter];
        gamal_generate_keys(key);

        sum_plain=0;
        for(int i=0; i<test_size; i++) {
            gamal_encrypt( ciphers[i], key, i);
            sum_plain += i;
        }


        for (int iteration = 0; iteration < experiment_iterations; iteration++) {
            ec_set_infty(sum->C1);
            ec_set_infty(sum->C2);
            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            for(int round=0; round<num_partitions; round++) {
                for (int sum_iter = 0; sum_iter < test_size; sum_iter++) {
                    gamal_add(sum, sum, ciphers[iteration]);
                }
            }
            high_resolution_clock::time_point t2 = high_resolution_clock::now();
            auto ns = duration_cast<nanoseconds>(t2-t1).count();
            if(iteration != experiment_iterations-1)
                std::cout << (unsigned long) ns << ", ";
            else
                std::cout << (unsigned long) ns << std::endl;
            gamal_decrypt(&res, key, sum, 1);
            if(!sum==sum_plain)
                std::cerr << "Sum wrong!" << std::endl;


        }

        gamal_key_clear(key);
        for(int i=0; i<test_size; i++) {
            gamal_cipher_clear(ciphers[i]);
        }
    }
}


int main(int argc, char* argv[]) {
    gamal_init();
    int steps[] = {50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 5000, 10000};
    int num_partitiions = 1;
    if(argc>=2) {
        num_partitiions= atoi(argv[1]);
    }
    benchmark(num_partitiions, steps, 17);
    gamal_deinit();
}