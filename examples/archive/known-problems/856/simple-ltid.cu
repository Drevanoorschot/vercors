//:: case SimpleLtid
//:: tools silicon
//:: verdict Pass

#include <cuda.h>

/*@
context_everywhere output != NULL;
requires get_global_size(0) == 1;
requires \pointer_index(output, \ltid, 1);
@*/
__global__ void CUDA_Kernel_Blelloch(int* output)
{
}
