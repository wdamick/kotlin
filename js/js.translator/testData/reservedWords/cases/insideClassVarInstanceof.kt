package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    var instanceof: Int = 0

    fun test() {
        testNotRenamed("instanceof", { instanceof })
    }
}

fun box(): String {
    TestClass().test()

    return "OK"
}