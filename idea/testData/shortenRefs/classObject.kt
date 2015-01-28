package p.q

<selection>fun foo(): Int {
    p.q.DefaultClassObject.foo()
    p.q.DefaultClassObject.coProp + 10
    p.q.DefaultClassObject.Default.foo()
    p.q.DefaultClassObject.Default
    p.q.DefaultClassObject

    p.q.NamedClassObject.foo()
    p.q.NamedClassObject.Named.foo()
    p.q.NamedClassObject.Named
    p.q.NamedClassObject
}</selection>

class DefaultClassObject {
    class object {
        val coProp = 1

        fun foo() {

        }
    }
}

class NamedClassObject {
    class object Named {
        fun foo() {

        }
    }
}
