#include <stdint.h>

__attribute__((noinline, visibility("default")))
int32_t appmetrica_fixture_add(int32_t left, int32_t right) {
    volatile int32_t result = left + right;
    return result;
}

__attribute__((noinline, visibility("default")))
int32_t appmetrica_fixture_fibonacci(int32_t value) {
    if (value < 2) {
        return value;
    }
    return appmetrica_fixture_fibonacci(value - 1) + appmetrica_fixture_fibonacci(value - 2);
}
