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
#include <chrono>

extern "C" {
#include "PollardKang.h"
}

using namespace std::chrono;
using namespace std;


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

void test_pollard(int num_rounds, int num_bits, int curve) {
    int i, tablesize, count=0;
    long result, temp;
    double sum = 0;
    PollardKang_state *state;
    EC_POINT *M;
    BIGNUM *num;
    EC_GROUP *group = EC_GROUP_new_by_curve_name(curve);
    num = BN_new();
    srand(time(NULL));
    result = -1;
    state = PollardKang_state_new(group, 1L << num_bits, 1L << (num_bits/2));
    tablesize = (int) state->table_size;
    cout << " Table Size: " << tablesize << " Points" << endl;
    for (i = 0; i < num_rounds; i++) {
        temp = rand() % (1L << num_bits);
        BN_set_word(num, (BN_ULONG) temp);
        M = multiply_generator(group, num);
        high_resolution_clock::time_point t1 = high_resolution_clock::now();
        if(compute_PollardKang(state, M, &result))
        {
            cout << "Error Occured Value Not Found: " << temp << endl;
            EC_POINT_free(M);
            continue;
        }
        high_resolution_clock::time_point t2 = high_resolution_clock::now();
        auto duration = duration_cast<milliseconds>(t2 - t1).count();
        sum += duration;
        count++;
        cout << (i+1) << (temp==result ? "" : " --Failed--") << " time: " << duration << "ms" << endl;
        EC_POINT_free(M);
    }
    PollardKang_state_free(state);
    cout << " average " << sum / count << "ms" << endl;
}

int main() {
    cout << "Start, Test!" << endl;
    test_pollard(100, 32, NID_X9_62_prime192v1);
    return 0;
}
