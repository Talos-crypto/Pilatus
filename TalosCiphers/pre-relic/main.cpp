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
#include "pre-hom.h"
#include "test_pre_udf.h"
}

using namespace std::chrono;

//Various DEBUG stuff
// Implements a short benchmark

int short_benchmark(int iter) {
    uint64_t msgs[iter], res;
    pre_keys_t alice_key, bob_key;
    pre_ciphertext_t alice_ciphers[iter], alice_ciphers_add[iter], bob_ciphers[iter];
    pre_re_token_t token_to_bob;
    double time;
    int ok = 1;

    pre_generate_keys(alice_key);
    pre_generate_keys(bob_key);

    for(int i=0; i<iter; i++) {
        msgs[i] = (uint64_t) i+1;
    }

    high_resolution_clock::time_point t1 = high_resolution_clock::now();
    for(int i=0; i<iter; i++) {
        pre_encrypt(alice_ciphers[i], alice_key, msgs[i]);
    }
    high_resolution_clock::time_point t2 = high_resolution_clock::now();
    auto ns = duration_cast<nanoseconds>(t2-t1).count();
    time = (double)ns / iter;
    std::cout << "Enc time is: " << time << " ns" << std::endl;

    for(int i=0; i<iter; i++) {
        pre_decrypt(&res, alice_key, alice_ciphers[i], 0);
        if(res!=i+1) {
            ok = 0;
            break;
        }
    }

    if(ok) {
        std::cout << "Decryption OK!" << std::endl;
    } else {
        std::cout << "Decryption Failed :(!" << std::endl;
    }

    t1 = high_resolution_clock::now();
    for(int i=0; i<iter; i++) {
        pre_ciphertext_init(alice_ciphers_add[i], alice_ciphers[0]->group);
        pre_homo_add(alice_key, alice_ciphers_add[i], alice_ciphers[i], alice_ciphers[(i+1) % iter], 0);
    }
    t2 = high_resolution_clock::now();
    ns = duration_cast<nanoseconds>(t2-t1).count();
    time = (double)ns / iter;
    std::cout << "Add time per element is: " << time << " ns" << std::endl;


    pre_generate_re_token(token_to_bob, alice_key, bob_key->pk_2);

    t1 = high_resolution_clock::now();
    for(int i=0; i<iter; i++) {
        pre_re_apply(token_to_bob, bob_ciphers[i], alice_ciphers[i]);
    }
    t2 = high_resolution_clock::now();
    ns = duration_cast<nanoseconds>(t2-t1).count();
    time = (double)ns / iter;
    std::cout << "RE-Enc time is: " << time << " ns" << std::endl;

    ok=1;
    for(int i=0; i<iter; i++) {
        pre_decrypt(&res, bob_key, bob_ciphers[i], 0);
        if(res!=i+1) {
            ok = 0;
            break;
        }
    }

    if(ok) {
        std::cout << "Decryption  Bob OK!" << std::endl;
    } else {
        std::cout << "Decryption Bob Failed :(!" << std::endl;
    }


}



int basic_test() {
    uint64_t msg1=23421345, msg2=50;
    pre_keys_t alice_key, bob_key;
    pre_ciphertext_t alice_cipher1, alice_cipher2, alice_add, bob_re;
    pre_re_token_t token_to_bob;
    dig_t res;

    pre_generate_keys(alice_key);
    pre_generate_keys(bob_key);

    pre_init_bsgs_table(1L<<18);

    pre_encrypt(alice_cipher1, alice_key, msg1);
    pre_encrypt(alice_cipher2, alice_key, msg2);

    pre_decrypt(&res, alice_key, alice_cipher1, 1);

    if(((uint64_t)res)==msg1) {
        std::cout << "OK!" << std::endl;
    } else {
        std::cout << "Failed!" << std::endl;
    }

    pre_ciphertext_init(alice_add, alice_cipher1->group);
    pre_homo_add(alice_key, alice_add, alice_cipher1, alice_cipher2, 0);

    pre_decrypt(&res, alice_key, alice_add, 1);

    if(((uint64_t)res)==(msg1+msg2)) {
        std::cout << "OK!" << std::endl;
    } else {
        std::cout << "Failed!" << std::endl;
    }

    pre_generate_re_token(token_to_bob, alice_key, bob_key->pk_2);

    pre_re_apply(token_to_bob, bob_re, alice_cipher1);

    pre_decrypt(&res, bob_key, bob_re, 1);

    if(((uint64_t)res)==(msg1)) {
        std::cout << "OK!" << std::endl;
    } else {
        std::cout << "Failed!" << std::endl;
    }


}

void encode_decode_test() {
    uint64_t msg1=23421345, msg2=50;
    pre_keys_t alice_key, bob_key, alice_key_decoded;
    pre_ciphertext_t alice_cipher1, alice_cipher1_decode, bob_re, bob_re_decode;
    pre_re_token_t token_to_bob, token_to_bob_decode;
    dig_t res;
    char* buff;
    int key_size;

    pre_generate_keys(alice_key);
    pre_generate_keys(bob_key);
    pre_generate_re_token(token_to_bob, alice_key, bob_key->pk_2);
    pre_encrypt(alice_cipher1, alice_key, msg1);
    key_size = get_encoded_key_size(alice_key);
    buff = (char *) malloc((size_t) key_size);
    if(!encode_key(buff, key_size, alice_key)==STS_OK) {
        std::cout << "Key encode error!" << std::endl;
        exit(1);
    }
    if(!decode_key(alice_key_decoded, buff, key_size)==STS_OK) {
        std::cout << "Key decode error!" << std::endl;
        exit(1);
    }
    free(buff);

    if(bn_cmp(alice_key->sk, alice_key_decoded->sk)==CMP_EQ &&
            gt_cmp(alice_key->Z, alice_key_decoded->Z)==CMP_EQ &&
            g1_cmp(alice_key->pk, alice_key_decoded->pk)==CMP_EQ &&
            g2_cmp(alice_key->pk_2, alice_key_decoded->pk_2)==CMP_EQ &&
            g1_cmp(alice_key->g, alice_key_decoded->g)==CMP_EQ &&
            g2_cmp(alice_key->g2, alice_key_decoded->g2)==CMP_EQ &&
            alice_key->type == alice_key_decoded->type){
        std::cout << "Decode Key OK!" << std::endl;
    } else {
        std::cout << "Decode Key Failed!" << std::endl;
    }

    key_size = get_encoded_token_size(token_to_bob);
    buff = (char *) malloc((size_t) key_size);
    encode_token(buff, key_size, token_to_bob);
    decode_token(token_to_bob_decode, buff, key_size);
    free(buff);

    if(g2_cmp(token_to_bob->re_token, token_to_bob_decode->re_token)==CMP_EQ) {
        std::cout << "Decode Token OK!" << std::endl;
    } else {
        std::cout << "Decode Token Failed!" << std::endl;
    }

    key_size = get_encoded_cipher_size(alice_cipher1);
    buff = (char *) malloc((size_t) key_size);
    encode_cipher(buff, key_size, alice_cipher1);
    decode_cipher(alice_cipher1_decode, buff, key_size);
    free(buff);

    if(gt_cmp(alice_cipher1->C1, alice_cipher1_decode->C1)==CMP_EQ
       && g1_cmp(alice_cipher1->C2_G1, alice_cipher1_decode->C2_G1)==CMP_EQ) {
        std::cout << "Decode Cipher OK!" << std::endl;
    } else {
        std::cout << "Decode Cipher Failed!" << std::endl;
    }

    pre_re_apply(token_to_bob, bob_re, alice_cipher1);

    key_size = get_encoded_cipher_size(bob_re);
    buff = (char *) malloc((size_t) key_size);
    encode_cipher(buff, key_size, bob_re);
    decode_cipher(bob_re_decode, buff, key_size);
    free(buff);

    if(gt_cmp(bob_re->C1, bob_re_decode->C1)==CMP_EQ &&
       gt_cmp(bob_re->C2_GT, bob_re_decode->C2_GT)==CMP_EQ) {
        std::cout << "Decode Cipher level2 OK!" << std::endl;
    } else {
        std::cout << "Decode Cipher level2 Failed!" << std::endl;
    }
    pre_init_bsgs_table(1L<<16);
    pre_decrypt(&res, alice_key, alice_cipher1, 1);
    if(res == msg1) {
        std::cout << "Dec OK!" << std::endl;
    } else {
        std::cout << "Dec Failed!" << std::endl;
    }

}

int test_brute() {
    gt_t G, brute;
    g1_t g1;
    g2_t g2;
    bn_t message, result;

    bn_new(message);
    bn_new(result);
    bn_read_str(message, "1000", 2, 10);

    g1_new(g1);
    g2_new(g2);
    gt_new(G);
    gt_new(brute);

    g1_get_gen(g1);
    g2_get_gen(g2);
    pc_map(G, g1, g2);

    gt_exp(brute, G, message);

    solve_dlog_brute(brute, G, result, 10000);

    if(bn_cmp(message, result)==CMP_EQ) {
        std::cout << "OK!" << std::endl;
    } else {
        std::cout << "Failed!" << std::endl;
    }
}

void write_size(char* buffer, int size) {
    buffer[0] = (char) ((size>>8) & 0xFF);
    buffer[1] = (char) (size & 0xFF);
}

int read_size(char* buffer) {
    return ((int)buffer[0] << 8) | buffer[1];
}

int test_udf() {
    uint64_t msg1=0, msg2=2;
    pre_keys_t key;
    sum_state *state,  *state_out;
    char* encoded;
    size_t enc_len;
    pre_ciphertext_t *cur_sum, *cursumout;
    int num;
    dig_t res1,res2,res3;

    state = (sum_state*) malloc(sizeof(sum_state));
    cur_sum = (pre_ciphertext_t*) malloc(sizeof(pre_ciphertext_t));
    pre_generate_keys(key);
    pre_encrypt(cur_sum[0], key, msg1);
    pre_decrypt(&res1, key, cur_sum[0], 1);
    state->num_ciphers=0;
    state->is_first=1;
    init_sum_state(state, 1, PRE_REL_CIPHERTEXT_IN_G_GROUP);

    encode_ciphers(&encoded, &enc_len, cur_sum, 1);
    decode_ciphers(encoded, enc_len, &cursumout, &num);
    pre_decrypt(&res2, key, cursumout[0], 1);

    addCiphers(cur_sum, cursumout, 1);
    pre_decrypt(&res3, key, cur_sum[0], 1);
    free_sum_state(state);



    return 0;
}

int test_udf_crt() {
    uint64_t msg1=0, msg2=2;
    pre_keys_t key;
    sum_state *state,  *state_out;
    char* encoded;
    size_t enc_len;
    pre_ciphertext_t *cur_sum, *cursumout;
    int num;
    dig_t res1,res2,res3;

    cur_sum = (pre_ciphertext_t*) malloc(2 * sizeof(pre_ciphertext_t));
    pre_generate_keys(key);
    pre_encrypt(cur_sum[0], key, msg1);
    pre_encrypt(cur_sum[1], key, msg1);

    encode_ciphers(&encoded, &enc_len, cur_sum, 2);
    decode_ciphers(encoded, enc_len, &cursumout, &num);
    addCiphers(cur_sum, cursumout, 2);

    return 0;
}

int load_int(uint64_t* res, bn_t num) {
    uint8_t temp[8];
    int iter=0;
    int size = bn_size_bin(num);
    int diff = 8-size;
    uint64_t result=0;
    int start=0;

    if(size>8) {
        start = size - 8;
    }
    bn_write_bin(temp, size, num);
    for(iter=start; iter<(size+start); iter++) {
        int shift = (64-(8*(diff+(1+(iter-start)))));
        result = result | (((uint64_t)temp[iter])<<shift);
    }
    *res=result;
    return STS_OK;
}

int test_func() {
    bn_t test;
    uint64_t res;
    bn_new(test);
    bn_set_dig(test, 1L<<54);
    load_int(&res, test);
    std::cout << "is: " <<  res << " expected: " << (1L<<54) <<std::endl;
}

int main() {
    pre_init();
    short_benchmark(1000);
    //basic_test();
    //encode_decode_test();
    //test_udf();
    //test_udf_crt();
    //test_func();
    pre_deinit();
    return 0;
}