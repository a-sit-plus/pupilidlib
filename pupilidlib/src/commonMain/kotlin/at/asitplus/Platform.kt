package at.asitplus

expect val platform: String

class Greeting {
    fun greeting() = "Hello, $platform!"
}