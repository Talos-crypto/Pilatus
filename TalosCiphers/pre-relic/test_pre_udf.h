//
// Created by lubu on 31.10.16.
//

#include "pre-hom.h"

#ifndef RELIC_HOM_TEST_PRE_UDF_H
#define RELIC_HOM_TEST_PRE_UDF_H

typedef struct {
    uint16_t is_first;
    int16_t num_ciphers;
    pre_ciphertext_t *cur_sum;
    char group;
} sum_state;

int encode_ciphers(char **encoded, size_t *encoded_len, pre_ciphertext_t *cipher, int num_ciphers);
int decode_ciphers(char *encoded, size_t encoded_len, pre_ciphertext_t **cipher, int *num_ciphers);
void init_sum_state(sum_state *state, int num_ciphers, char group);
void addCiphers(pre_ciphertext_t *cur, pre_ciphertext_t *updated, int num);
void free_sum_state(sum_state *state);

#endif //RELIC_HOM_TEST_PRE_UDF_H


