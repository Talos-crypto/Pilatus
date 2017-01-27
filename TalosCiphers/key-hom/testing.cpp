//
// Created by Lukas Burkhalter on 15.07.16.
//
#include <iostream>
#include <chrono>
#include <cstring>

extern "C" {
#include "key_hom.h"
}

using namespace std::chrono;
using namespace std;;

int check_equality(unsigned char *data1, unsigned char *data2, int data_len) {
    int iter_len;
    for(iter_len=0; iter_len<data_len; iter_len++) {
        if(data1[iter_len]!=data2[iter_len]) {
            return 0;
        }

    }
    return 1;
}

int check_equality_full(unsigned char **data1, unsigned char **data2, int data_num, int data_len) {
    int iter_num;
    for(iter_num=0; iter_num<data_num; iter_num++) {
        if(!check_equality(data1[iter_num], data2[iter_num], data_len)) {
            return 0;
        }
    }
    return 1;
}

void print_bytes(unsigned char *data1, int data_len) {
    size_t i = 0;
    for(i = 0; i < data_len; ++i)
        fprintf(stdout, "%02X%s", data1[i],
                ( i + 1 ) % 16 == 0 ? "\r\n" : " " );
}

int perform_test(int num_ciphers, size_t data_size) {
    int iter;
    unsigned char *plaintexts[num_ciphers], *plaintexts_res_k1[num_ciphers], *plaintexts_res_k2[num_ciphers];
    KHC_Cipher *ciphers[num_ciphers], *re_enc_ciphers[num_ciphers];
    KHC_Key *key1, *key2, *key1_to_key2;
    size_t temporary;
    double sum_enc=0, sum_dec=0, sum_re_enc=0, sum_token_gen=0;
    high_resolution_clock::time_point t1, t2;

    cout << "-----Test-Log-" << data_size << "-byte-----" << endl;

    // create keys
    cout << "create keys\n";
    key1 = KHC_new_key();
    key2 = KHC_new_key();

    t1 = high_resolution_clock::now();
    KHC_create_re_enc_token(key1, key2, &key1_to_key2);
    t2 = high_resolution_clock::now();
    auto duration = duration_cast<milliseconds>(t2 - t1).count();
    sum_token_gen+=duration;

    // create data
    cout << "create " << num_ciphers << " plaintexts with length " << data_size << " bytes\n";
    for (iter = 0; iter < num_ciphers; iter++) {
        plaintexts[iter] = (unsigned char *) malloc(data_size);
        RAND_bytes(plaintexts[iter], (int) data_size);
    }

    // encrypt data key1
    cout << "encrypt data key1\n";
    for (iter = 0; iter < num_ciphers; iter++) {
        t1 = high_resolution_clock::now();
        KHC_encrypt(plaintexts[iter], data_size, key1, 0, &ciphers[iter]);
        t2 = high_resolution_clock::now();
        auto duration = duration_cast<milliseconds>(t2 - t1).count();
        sum_enc+=duration;
    }

    // decrypt data key1
    cout << "decrypt data key1\n";
    for (iter = 0; iter < num_ciphers; iter++) {
        t1 = high_resolution_clock::now();
        KHC_decrypt(ciphers[iter], key1, &plaintexts_res_k1[iter], &temporary);
        t2 = high_resolution_clock::now();
        auto duration = duration_cast<milliseconds>(t2 - t1).count();
        sum_dec+=duration;
        if (temporary != data_size) {
            cout << "on iteration " << iter << " size missmage have: " << temporary << " expected: " << data_size;
        }
    }

    if (check_equality_full(plaintexts, plaintexts_res_k1, num_ciphers, (int) data_size)) {
        cout << "Sucessfull decryption under key1\n";
    } else {
        cout << "ERROR decryption under key1 -> missmach\n";
    }

    // re-encrypt to key2
    cout << "re-encrypt to key2\n";
    for (iter = 0; iter < num_ciphers; iter++) {
        t1 = high_resolution_clock::now();
        KHC_re_encrypt(ciphers[iter], key1_to_key2, &re_enc_ciphers[iter]);
        t2 = high_resolution_clock::now();
        auto duration = duration_cast<milliseconds>(t2 - t1).count();
        sum_re_enc+=duration;
    }

    // decrypt key2
    cout << "decrypt data key2\n";
    for (iter = 0; iter < num_ciphers; iter++) {
        KHC_decrypt(re_enc_ciphers[iter], key2, &plaintexts_res_k2[iter], &temporary);
        if (temporary != data_size) {
            cout << "on iteration " << iter << "size missmage have: " << temporary << " expected: " << data_size << "\n";
        }
    }

    if (check_equality_full(plaintexts, plaintexts_res_k2, num_ciphers, (int) data_size)) {
        cout << "Sucessfull decryption under key2\n";
    } else {
        cout << "ERROR decryption under key2 -> missmach\n";
    }
    cout.precision(6);
    cout << "----Average Times----" << endl;
    cout << "Encryption: " << sum_enc / num_ciphers << " ms" <<endl;
    cout << "Decryption: " << sum_dec / num_ciphers << " ms" << endl;
    cout << "Re-Encryption: " << sum_re_enc / num_ciphers << " ms" << endl;
    cout << "Token-creation: " << sum_token_gen << " ms" << endl;
    return 0;
}

int cmp_ciphers(EC_GROUP *group, KHC_Cipher *c_before, KHC_Cipher *c_after) {
    int iter;
    BN_CTX *ctx;

    if(c_before->iv_length!=c_after->iv_length) {
        return 0;
    }

    if(c_before->num_points!=c_after->num_points) {
        return 0;
    }

    if(memcmp(c_before->iv, c_after->iv, c_before->iv_length)!=0) {
        return 0;
    }

    ctx = BN_CTX_new();

    for(iter=0; iter<c_before->num_points; iter++) {
        if(EC_POINT_cmp(group, (c_before->points)[iter], (c_after->points)[iter], ctx)!=0) {
            BN_CTX_free(ctx);
            return 0;
        }
    }
    BN_CTX_free(ctx);
    return 1;
}

int test_encode_decode() {
    KHC_Key *key1, *key2;
    KHC_Cipher *c_before, *c_after;
    unsigned char temp[30];
    unsigned char *k_buf, *c_buf;
    size_t k_buf_len, c_buf_len;
    BN_CTX *ctx = BN_CTX_new();

    cout << "----Key Encode/Decode Test----" << endl;
    key1 = KHC_new_key();
    KHC_Key_encode(key1, &k_buf, &k_buf_len);
    KHC_Key_decode(k_buf, k_buf_len, &key2);

    cout << "Key Before: " << BN_bn2dec(key1->key) << " Key After: " << BN_bn2dec(key2->key) << endl;

    if(BN_cmp(key1->key, key2->key)==0 && EC_GROUP_cmp(key1->group, key2->group, ctx)==0) {
        cout << "Key encode/decode successfull :) " << endl;
    } else {
        cout << "Key encode/decode failed :( " << endl;
    }

    cout << "----Cipher Encode/Decode Test----" << endl;
    RAND_bytes(temp, 30);
    KHC_encrypt(temp, 30, key1, 0, &c_before);

    KHC_Cipher_encode(key1->group, c_before, &c_buf, &c_buf_len);
    KHC_Cipher_decode(key1->group, c_buf, c_buf_len, &c_after);

    if(cmp_ciphers(key1->group, c_before, c_after)) {
        cout << "Cipher encode/decode successfull :) " << endl;
    } else {
        cout << "Key encode/decode failed :( " << endl;
    }

    KHC_Cipher_clear(c_before);
}


int main(int argc, char* argv[]) {
    int arg1, arg2;
    if (argc < 3) {
        cout << "Usage: " << argv[0] << "NUM_ITERATIONS NUM_BYTES_DATA" << std::endl;
        cout << "Use Default Params" << endl;
        arg1 = 1000;
        arg2 = 200;
    } else {
        arg1 = std::stoi(argv[1]);
        arg2 = std::stoi(argv[2]);
    }
    perform_test(arg1, (size_t) arg2);
    test_encode_decode();
}