package p.q

<selection>fun foo(): Int {
    p.q.InnerObj.D.foo()
    p.q.InnerObj.D.coProp + 10
    p.q.InnerObj.D

    p.q.TopLevel.foo()
    p.q.TopLevel
}</selection>

class InnerObj {
    object D {
        val coProp = 1

        fun foo() {

        }
    }
}

object TopLevel {
    fun foo() {}
}
