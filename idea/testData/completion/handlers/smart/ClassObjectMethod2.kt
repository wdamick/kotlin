package sample

class K {
    class object {
        fun bar(p: Int): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: bar
