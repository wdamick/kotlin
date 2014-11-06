// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
package server

trait Some {
    class object {
        val <caret>XX = 1
    }
}

val a = Some.XX