/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef OS_CPU_WINDOWS_X86_VM_ATOMIC_WINDOWS_X86_HPP
#define OS_CPU_WINDOWS_X86_VM_ATOMIC_WINDOWS_X86_HPP

#include "runtime/os.hpp"

// The following alternative implementations are needed because
// Windows 95 doesn't support (some of) the corresponding Windows NT
// calls. Furthermore, these versions allow inlining in the caller.
// (More precisely: The documentation for InterlockedExchange says
// it is supported for Windows 95. However, when single-stepping
// through the assembly code we cannot step into the routine and
// when looking at the routine address we see only garbage code.
// Better safe then sorry!). Was bug 7/31/98 (gri).
//
// Performance note: On uniprocessors, the 'lock' prefixes are not
// necessary (and expensive). We should generate separate cases if
// this becomes a performance problem.

#pragma warning(disable: 4035) // Disables warnings reporting missing return statement

inline void Atomic::store    (jbyte    store_value, jbyte*    dest) { *dest = store_value; }
inline void Atomic::store    (jshort   store_value, jshort*   dest) { *dest = store_value; }
inline void Atomic::store    (jint     store_value, jint*     dest) { *dest = store_value; }

inline void Atomic::store_ptr(intptr_t store_value, intptr_t* dest) { *dest = store_value; }
inline void Atomic::store_ptr(void*    store_value, void*     dest) { *(void**)dest = store_value; }

inline void Atomic::store    (jbyte    store_value, volatile jbyte*    dest) { *dest = store_value; }
inline void Atomic::store    (jshort   store_value, volatile jshort*   dest) { *dest = store_value; }
inline void Atomic::store    (jint     store_value, volatile jint*     dest) { *dest = store_value; }


inline void Atomic::store_ptr(intptr_t store_value, volatile intptr_t* dest) { *dest = store_value; }
inline void Atomic::store_ptr(void*    store_value, volatile void*     dest) { *(void* volatile *)dest = store_value; }

template<size_t byte_size>
struct Atomic::PlatformAdd
  : Atomic::AddAndFetch<Atomic::PlatformAdd<byte_size> >
{
  template<typename I, typename D>
  D add_and_fetch(I add_value, D volatile* dest) const;
};

#ifdef AMD64
inline void Atomic::store    (jlong    store_value, jlong*    dest) { *dest = store_value; }
inline void Atomic::store    (jlong    store_value, volatile jlong*    dest) { *dest = store_value; }

template<>
template<typename I, typename D>
inline D Atomic::PlatformAdd<4>::add_and_fetch(I add_value, D volatile* dest) const {
  return add_using_helper<jint>(os::atomic_add_func, add_value, dest);
}

template<>
template<typename I, typename D>
inline D Atomic::PlatformAdd<8>::add_and_fetch(I add_value, D volatile* dest) const {
  return add_using_helper<intptr_t>(os::atomic_add_ptr_func, add_value, dest);
}

inline void Atomic::inc    (volatile jint*     dest) {
  (void)add    (1, dest);
}

inline void Atomic::inc_ptr(volatile intptr_t* dest) {
  (void)add_ptr(1, dest);
}

inline void Atomic::inc_ptr(volatile void*     dest) {
  (void)add_ptr(1, dest);
}

inline void Atomic::dec    (volatile jint*     dest) {
  (void)add    (-1, dest);
}

inline void Atomic::dec_ptr(volatile intptr_t* dest) {
  (void)add_ptr(-1, dest);
}

inline void Atomic::dec_ptr(volatile void*     dest) {
  (void)add_ptr(-1, dest);
}

inline jint     Atomic::xchg    (jint     exchange_value, volatile jint*     dest) {
  return (jint)(*os::atomic_xchg_func)(exchange_value, dest);
}

inline intptr_t Atomic::xchg_ptr(intptr_t exchange_value, volatile intptr_t* dest) {
  return (intptr_t)(os::atomic_xchg_ptr_func)(exchange_value, dest);
}

inline void*    Atomic::xchg_ptr(void*    exchange_value, volatile void*     dest) {
  return (void *)(os::atomic_xchg_ptr_func)((intptr_t)exchange_value, (volatile intptr_t*)dest);
}

#define DEFINE_STUB_CMPXCHG(ByteSize, StubType, StubName)               \
  template<>                                                            \
  template<typename T>                                                  \
  inline T Atomic::PlatformCmpxchg<ByteSize>::operator()(T exchange_value, \
                                                         T volatile* dest, \
                                                         T compare_value, \
                                                         cmpxchg_memory_order order) const { \
    STATIC_ASSERT(ByteSize == sizeof(T));                               \
    return cmpxchg_using_helper<StubType>(StubName, exchange_value, dest, compare_value); \
  }

DEFINE_STUB_CMPXCHG(1, jbyte, os::atomic_cmpxchg_byte_func)
DEFINE_STUB_CMPXCHG(4, jint,  os::atomic_cmpxchg_func)
DEFINE_STUB_CMPXCHG(8, jlong, os::atomic_cmpxchg_long_func)

#undef DEFINE_STUB_CMPXCHG

inline jlong Atomic::load(const volatile jlong* src) { return *src; }

#else // !AMD64

template<>
template<typename I, typename D>
inline D Atomic::PlatformAdd<4>::add_and_fetch(I add_value, D volatile* dest) const {
  STATIC_ASSERT(4 == sizeof(I));
  STATIC_ASSERT(4 == sizeof(D));
  __asm {
    mov edx, dest;
    mov eax, add_value;
    mov ecx, eax;
    lock xadd dword ptr [edx], eax;
    add eax, ecx;
  }
}

inline void Atomic::inc    (volatile jint*     dest) {
  // alternative for InterlockedIncrement
  __asm {
    mov edx, dest;
    lock add dword ptr [edx], 1;
  }
}

inline void Atomic::inc_ptr(volatile intptr_t* dest) {
  inc((volatile jint*)dest);
}

inline void Atomic::inc_ptr(volatile void*     dest) {
  inc((volatile jint*)dest);
}

inline void Atomic::dec    (volatile jint*     dest) {
  // alternative for InterlockedDecrement
  __asm {
    mov edx, dest;
    lock sub dword ptr [edx], 1;
  }
}

inline void Atomic::dec_ptr(volatile intptr_t* dest) {
  dec((volatile jint*)dest);
}

inline void Atomic::dec_ptr(volatile void*     dest) {
  dec((volatile jint*)dest);
}

inline jint     Atomic::xchg    (jint     exchange_value, volatile jint*     dest) {
  // alternative for InterlockedExchange
  __asm {
    mov eax, exchange_value;
    mov ecx, dest;
    xchg eax, dword ptr [ecx];
  }
}

inline intptr_t Atomic::xchg_ptr(intptr_t exchange_value, volatile intptr_t* dest) {
  return (intptr_t)xchg((jint)exchange_value, (volatile jint*)dest);
}

inline void*    Atomic::xchg_ptr(void*    exchange_value, volatile void*     dest) {
  return (void*)xchg((jint)exchange_value, (volatile jint*)dest);
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<1>::operator()(T exchange_value,
                                                T volatile* dest,
                                                T compare_value,
                                                cmpxchg_memory_order order) const {
  STATIC_ASSERT(1 == sizeof(T));
  // alternative for InterlockedCompareExchange
  __asm {
    mov edx, dest
    mov cl, exchange_value
    mov al, compare_value
    lock cmpxchg byte ptr [edx], cl
  }
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T exchange_value,
                                                T volatile* dest,
                                                T compare_value,
                                                cmpxchg_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  // alternative for InterlockedCompareExchange
  __asm {
    mov edx, dest
    mov ecx, exchange_value
    mov eax, compare_value
    lock cmpxchg dword ptr [edx], ecx
  }
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T exchange_value,
                                                T volatile* dest,
                                                T compare_value,
                                                cmpxchg_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  jint ex_lo  = (jint)exchange_value;
  jint ex_hi  = *( ((jint*)&exchange_value) + 1 );
  jint cmp_lo = (jint)compare_value;
  jint cmp_hi = *( ((jint*)&compare_value) + 1 );
  __asm {
    push ebx
    push edi
    mov eax, cmp_lo
    mov edx, cmp_hi
    mov edi, dest
    mov ebx, ex_lo
    mov ecx, ex_hi
    lock cmpxchg8b qword ptr [edi]
    pop edi
    pop ebx
  }
}

inline jlong Atomic::load(const volatile jlong* src) {
  volatile jlong dest;
  volatile jlong* pdest = &dest;
  __asm {
    mov eax, src
    fild     qword ptr [eax]
    mov eax, pdest
    fistp    qword ptr [eax]
  }
  return dest;
}

inline void Atomic::store(jlong store_value, volatile jlong* dest) {
  volatile jlong* src = &store_value;
  __asm {
    mov eax, src
    fild     qword ptr [eax]
    mov eax, dest
    fistp    qword ptr [eax]
  }
}

inline void Atomic::store(jlong store_value, jlong* dest) {
  Atomic::store(store_value, (volatile jlong*)dest);
}

#endif // AMD64

#pragma warning(default: 4035) // Enables warnings reporting missing return statement

#endif // OS_CPU_WINDOWS_X86_VM_ATOMIC_WINDOWS_X86_HPP
