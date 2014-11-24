open class Bar<T>(val prop: String)
class Foo {
    class object : Bar<Foo>("OK")

    val p = Foo.prop
}
fun box(): String = Foo.prop