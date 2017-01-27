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

//
// Partly ported from 
// https://github.com/miracl/MIRACL/blob/master/source/kangaroo.c
//

#include "PollardKang.h"

BIGNUM *create_BN(long i) {
    BIGNUM *bnI;
    bnI = BN_new();
    BN_set_word(bnI, (BN_ULONG) i);
    return bnI;
}

EC_POINT *multiply_const(EC_GROUP *curve_group, const EC_POINT *in, const BIGNUM *n) {
    EC_POINT *res;
    BIGNUM *bn1;
    BN_CTX *ctx;
    bn1 = BN_new();
    ctx = BN_CTX_new();
    BN_zero(bn1);
    res = EC_POINT_new(curve_group);
    EC_POINT_mul(curve_group, res, bn1, in, n, ctx);
    BN_free(bn1);
    BN_CTX_free(ctx);
    return res;
}

EC_POINT *multiply_generator(EC_GROUP *curve_group, const BIGNUM *n) {
    return multiply_const(curve_group, EC_GROUP_get0_generator(curve_group), n);
}

/**
 * Maps EC Point to a integer in interval [0, m-1]
 * Computes xcord(in) + ycord(in) mod m
 */
int rand_func(EC_GROUP *curve_group, EC_POINT *in, int m) {
    BIGNUM *x, *y;
    BN_ULONG res;
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    x = BN_new();
    y = BN_new();
    EC_POINT_get_affine_coordinates_GF2m(curve_group, in, x, y, ctx);
    BN_add(x, x, y); //  x= x-Cord+y-Cord
    res = BN_mod_word(x, (BN_ULONG) m); // x = x mod m
    BN_CTX_free(ctx);
    BN_free(x);
    BN_free(y);
    return (int) res;
}

void EC_table_free(EC_POINT **table, size_t size) {
    EC_POINT *temp;
    int i = 0;
    for (; i < size; i++) {
        temp = table[i];
        EC_POINT_free(temp);
    }
    free(table);
}

PollardKang_state *PollardKang_state_new(EC_GROUP *curve_group, long limit, long leaps) {
    BIGNUM *t;
    EC_POINT *x, **table;
    int i, j, m;
    long dn, s, *distance;
    BN_CTX *ctx;
    PollardKang_state *state = (PollardKang_state *) malloc(sizeof(PollardKang_state));

    ctx = BN_CTX_new();
    t = BN_new();

    state->limit = limit;
    state->leaps = leaps;
    state->group = curve_group;

    for (s = 1L, m = 1; ; m++) { /* find table size */
        s *= 2;
        if ((2 * s / m) > (leaps / 4)) break;
    }

    table = (EC_POINT **) malloc(m * sizeof(EC_POINT *));
    distance = (long *) malloc(m * sizeof(long));

    for (s = 1L, m = 1; ; m++) {
        distance[m - 1] = s;
        s *= 2;
        if ((2 * s / m) > (leaps / 4)) break;
    }

    state->table_size = (size_t) m;

    for (i = 0; i < m; i++) { /* create table  Ti */
        BN_set_word(t, (BN_ULONG) distance[i]);
        table[i] = multiply_generator(curve_group, t);
    }

    BN_set_word(t, (BN_ULONG) limit); //  transform LIMIT to t bigint
    x = multiply_generator(curve_group, t);

    for (dn = 0L, j = 0; j < leaps; j++) {
        i = rand_func(curve_group, x, m);  /* random function */
        EC_POINT_add(curve_group, x, x, table[i], ctx); // X = X + Ti
        dn += distance[i]; // dn = di + dn
    }

    state->trap = x; // Trap = X
    state->table = table;
    state->distance = distance;
    state->dn = dn;

    BN_CTX_free(ctx);
    BN_free(t);
    return state;
}

void PollardKang_state_free(PollardKang_state *state) {
    EC_table_free(state->table, state->table_size);
    free(state->distance);
    EC_POINT_free(state->trap);
    EC_GROUP_free(state->group);
    free(state);
}

int compute_PollardKang(PollardKang_state *state, EC_POINT *msg, long *result) {
    EC_POINT *x, *trap, **table;
    int i, m;
    long dm, dn, *distance;
    BN_CTX *ctx;

    ctx = BN_CTX_new();
    table = state->table;
    distance = state->distance;
    trap = state->trap;
    dn = state->dn;
    m = (int) state->table_size;

    x = EC_POINT_dup(msg, state->group); // x = M <- res*G=M

    for (dm = 0L; ;) {
        i = rand_func(state->group, x, m);  /* random function */
        EC_POINT_add(state->group, x, x, table[i], ctx); // X = X + Ti
        dm += distance[i]; //dm = di + dm
        if (EC_POINT_cmp(state->group, x, trap, ctx) == 0 || dm > state->limit + dn) break; // X == Trap ?
    }

    if (dm > state->limit + dn) { /* not found :( */
        return -1;
    }

    *result = state->limit + dn - dm;

    BN_CTX_free(ctx);
    EC_POINT_free(x);
    return 0;
}
