#include <gmp.h>
#include <pbc/pbc.h>

int main()
{
	FILE * out_params;
	pairing_t pairing;
	pbc_param_t	params;
	out_params = fopen("./param.a", "w");
	pbc_param_init_a_gen(params, 160, 1024);
	pbc_param_out_str(out_params, params);
	fclose(out_params);
	pbc_param_clear(params);
	pairing_clear(pairing);
    return 0;
}