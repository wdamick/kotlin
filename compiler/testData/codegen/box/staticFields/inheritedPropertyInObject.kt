open class Bar<T>(val prop: String)
object Foo : Bar<Foo>("OK") {

    val p = Foo.prop
}
fun box(): String = Foo.prop