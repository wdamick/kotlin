// !DIAGNOSTICS: -UNUSED_PARAMETER

trait B<T> {
    fun foo(t: T) {}
}

class C : B<String> {
    <!CONFLICTING_JVM_DECLARATIONS!>override fun foo(t: String)<!> {}

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(o: Any)<!> {}
}