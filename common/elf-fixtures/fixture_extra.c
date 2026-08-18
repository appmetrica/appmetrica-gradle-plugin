#include <stdint.h>

__attribute__((noinline, visibility("default")))
int32_t appmetrica_fixture_multiply(int32_t left, int32_t right) {
    volatile int32_t result = left * right;
    return result;
}
