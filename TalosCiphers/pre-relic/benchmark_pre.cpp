//
// Created by lubu on 07.12.16.
//
#include <iostream>
#include <gmpxx.h>
#include <chrono>

extern "C" {
#include "pre-hom.h"
#include "test_pre_udf.h"
}

using namespace std::chrono;

//Implements the a benchmark for PRE

int short_benchmark(int iter, int num_partitions) {
    uint64_t msgs[iter], res;
    pre_keys_t alice_key, bob_key;
    pre_ciphertext_t alice_ciphers[iter], alice_ciphers_add[iter], bob_ciphers[iter];
    pre_re_token_t token_to_bob;
    double time;

    std::cout << "re-encryption time" << std::endl;

    pre_generate_keys(alice_key);
    pre_generate_keys(bob_key);

    for(int i=0; i<iter; i++) {
        msgs[i] = (uint64_t) i+1;
    }

    for(int i=0; i<iter; i++) {
        pre_encrypt(alice_ciphers[i], alice_key, msgs[i]);
    }

    pre_generate_re_token(token_to_bob, alice_key, bob_key->pk_2);

    for(int i=0; i<iter; i++) {
        high_resolution_clock::time_point t1 = high_resolution_clock::now();
        for(int count=0; count<num_partitions; count ++) {
            pre_re_apply(token_to_bob, bob_ciphers[i], alice_ciphers[i]);
        }
        high_resolution_clock::time_point t2 = high_resolution_clock::now();
        auto ns = duration_cast<nanoseconds>(t2-t1).count();
        std::cout << (uint64_t) ns << std::endl;
    }
}

int main(int argc, char* argv[]) {
    int num_partitiions = 1;
    pre_init();
    if(argc>=2) {
        num_partitiions= atoi(argv[1]);
    }
    short_benchmark(1000, num_partitiions);
    pre_deinit();
    return 0;
}