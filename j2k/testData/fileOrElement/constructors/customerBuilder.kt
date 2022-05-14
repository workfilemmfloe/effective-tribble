package org.test.customer

class Customer(public val firstName: String, public val lastName: String) {

    init {
        doSmthBefore()
        doSmthAfter()
    }

    private fun doSmthBefore() {
    }

    private fun doSmthAfter() {
    }
}

class CustomerBuilder {
    public var _firstName: String = "Homer"
    public var _lastName: String = "Simpson"

    public fun WithFirstName(firstName: String): CustomerBuilder {
        _firstName = firstName
        return this
    }

    public fun WithLastName(lastName: String): CustomerBuilder {
        _lastName = lastName
        return this
    }

    public fun Build(): Customer {
        return Customer(_firstName, _lastName)
    }
}

public object User {
    public fun main() {
        val customer = CustomerBuilder().WithFirstName("Homer").WithLastName("Simpson").Build()
        println(customer.firstName)
        println(customer.lastName)
    }
}